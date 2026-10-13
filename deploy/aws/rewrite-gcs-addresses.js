// Rewrites every stored address of a file in the GCS bucket to that file's address in the S3 bucket
// it was copied into (README, "Files already on GCS"), so the GCS bucket can be deleted. Run it once
// gcsFilesReadFrom is s3, from a machine that can reach the database:
//
//   GCS_BUCKET=<gcpBucketName> S3_BUCKET=<s3BucketName> S3_REGION=ap-south-1 DB_NAME=komposeauth \
//     mongosh "$MONGODB_URI" --quiet --file deploy/aws/rewrite-gcs-addresses.js
//
// A dry run unless APPLY=1 is set too. GcsAddressesOnS3 already reads and deletes an old address in
// S3; what it cannot answer is a profile photo or an organization logo, which a browser loads
// straight from the address on the record. Those are why the GCS bucket outlives the move, and
// rewriting them retires it.
//
// Every collection and every string is walked rather than a list of known fields, so a field added
// later is not missed. A value is rewritten only when the whole of it is one address in this bucket;
// an address inside longer text, or in another bucket, is counted and left as written.

// URLEncoder.encode per segment, as S3StorageService writes a key: only A-Z a-z 0-9 . - * _ stay as
// they are, so the address is the one S3StorageService.upload would have recorded.
function encodePath(name) {
  return name.split('/')
    .map(segment => encodeURIComponent(segment)
      .replace(/[!'()~]/g, c => '%' + c.charCodeAt(0).toString(16).toUpperCase()))
    .join('/');
}

// URLDecoder.decode, which reads '+' as a space.
function decode(encoded) {
  try {
    return decodeURIComponent(encoded.replace(/\+/g, ' '));
  } catch (e) {
    return null;
  }
}

function makeRewriter({gcsBucket, s3Bucket, s3Region}) {
  const bucketUrl = `https://${s3Bucket}.s3.${s3Region}.amazonaws.com`;
  const mediaLink = /^https:\/\/storage\.googleapis\.com\/download\/storage\/v1\/b\/([^/]+)\/o\/([^?#]+)/;
  const plain = /^(?:gs:\/\/|https:\/\/storage\.googleapis\.com\/)([^/?#]+)\/([^?#]+)/;

  // The same two forms GcpStorageService.blobNameIn reads.
  function read(value) {
    if (typeof value !== 'string') return null;
    const match = mediaLink.exec(value) || plain.exec(value);
    if (!match) {
      const mentioned = value.includes(gcsBucket) &&
        (value.includes('storage.googleapis.com') || value.includes('gs://'));
      return mentioned ? {kind: 'inText'} : null;
    }
    if (match[1] !== gcsBucket) return {kind: 'otherBucket'};
    if (/\s/.test(value)) return {kind: 'inText'};
    const name = decode(match[2]);
    if (!name || name.split('/').some(segment => segment === '..')) return {kind: 'unreadable'};
    return {kind: 'rewrite', name, address: `${bucketUrl}/${encodePath(name)}`};
  }

  return {read, bucketUrl};
}

// Every string in a document with the path that reaches it. The top-level _id is never an address.
function* strings(value, path) {
  if (typeof value === 'string') {
    yield [path, value];
    return;
  }
  if (value === null || typeof value !== 'object' || value._bsontype || value instanceof Date) return;
  if (Array.isArray(value)) {
    for (let i = 0; i < value.length; i++) yield* strings(value[i], path.concat(String(i)));
    return;
  }
  for (const key of Object.keys(value)) {
    if (path.length === 0 && key === '_id') continue;
    yield* strings(value[key], path.concat(key));
  }
}

// An update path cannot name a key with a dot in it or one starting with '$'.
const addressable = path => path.every(key => key !== '' && !key.includes('.') && !key.startsWith('$'));

function main(connected) {
  const env = process.env;
  const need = name => {
    if (!env[name]) throw new Error(`${name} is not set`);
    return env[name];
  };
  const apply = env.APPLY === '1';
  const target = connected.getSiblingDB(need('DB_NAME'));
  const rewriter = makeRewriter({
    gcsBucket: need('GCS_BUCKET'),
    s3Bucket: need('S3_BUCKET'),
    s3Region: env.S3_REGION || 'ap-south-1'
  });
  print(`${apply ? 'Rewriting' : 'Dry run, nothing is written (APPLY=1 writes)'}: gs://${env.GCS_BUCKET} -> ${rewriter.bucketUrl} in ${target.getName()}`);

  const found = {};
  const tally = (where, kind, sample) => {
    const row = found[where] || (found[where] = {rewrite: 0, inText: 0, otherBucket: 0, unreadable: 0, unaddressable: 0, written: 0, samples: []});
    row[kind]++;
    if (sample && row.samples.length < 2) row.samples.push(sample);
    return row;
  };
  const labelOf = (collection, path) => `${collection}.${path.map(key => /^\d+$/.test(key) ? '$' : key).join('.')}`;

  const collections = target.getCollectionInfos({type: 'collection'}, {nameOnly: true, authorizedCollections: true})
    .map(info => info.name)
    .filter(name => !name.startsWith('system.'))
    .sort();

  for (const name of collections) {
    const collection = target.getCollection(name);
    let batch = [];
    const flush = () => {
      if (batch.length === 0) return;
      const result = collection.bulkWrite(batch.map(op => op.op), {ordered: false});
      if (result.modifiedCount !== batch.length) {
        print(`  ${name}: ${batch.length - result.modifiedCount} value(s) changed while this ran and were left alone`);
      }
      batch.forEach(op => op.row.written++);
      batch = [];
    };

    collection.find({}).batchSize(500).forEach(doc => {
      for (const [path, value] of strings(doc, [])) {
        const seen = rewriter.read(value);
        if (!seen) continue;
        const where = labelOf(name, path);
        if (seen.kind !== 'rewrite') {
          tally(where, seen.kind, value.length > 160 ? `${value.slice(0, 160)}…` : value);
          continue;
        }
        if (!addressable(path)) {
          tally(where, 'unaddressable', value);
          continue;
        }
        const row = tally(where, 'rewrite', `${value} -> ${seen.address}`);
        if (!apply) continue;
        const field = path.join('.');
        batch.push({
          row,
          op: {updateOne: {filter: {_id: doc._id, [field]: value}, update: {$set: {[field]: seen.address}}}}
        });
        if (batch.length >= 500) flush();
      }
    });
    flush();
  }

  const rows = Object.entries(found);
  if (rows.length === 0) {
    print(`No address in gs://${env.GCS_BUCKET} is stored in ${target.getName()}.`);
    return;
  }
  let toRewrite = 0;
  let left = 0;
  for (const [where, row] of rows) {
    toRewrite += row.rewrite;
    left += row.inText + row.otherBucket + row.unreadable + row.unaddressable;
    const parts = [];
    if (row.rewrite) parts.push(apply ? `${row.written} of ${row.rewrite} rewritten` : `${row.rewrite} to rewrite`);
    if (row.inText) parts.push(`${row.inText} inside longer text, left as written`);
    if (row.otherBucket) parts.push(`${row.otherBucket} in another Google bucket, left as written`);
    if (row.unreadable) parts.push(`${row.unreadable} unreadable, left as written`);
    if (row.unaddressable) parts.push(`${row.unaddressable} under a key an update cannot name, left as written`);
    print(`${where}: ${parts.join('; ')}`);
    row.samples.forEach(sample => print(`    ${sample}`));
  }
  print(`${toRewrite} address(es) ${apply ? 'found and rewritten' : 'to rewrite'}, ${left} Google address(es) or mention(s) left as written.`);
  if (apply) print('Run it again without APPLY: it should find nothing left to rewrite.');
}

// mongosh defines print; node, which the checks of the functions above run under, does not.
if (typeof print === 'function') main(db);
else module.exports = {encodePath, makeRewriter, strings, addressable};
