package pitampoudel.komposeauth.organization.service


import org.bson.types.ObjectId
import pitampoudel.core.data.PhoneNumber
import pitampoudel.core.data.parsePhoneNumber
import pitampoudel.komposeauth.organization.data.CreateOrUpdateOrganizationRequest
import pitampoudel.komposeauth.organization.data.OrganizationResponse
import pitampoudel.komposeauth.organization.entity.Organization
import kotlin.time.Clock

fun CreateOrUpdateOrganizationRequest.toOrganization(
    userId: ObjectId,
    logoImageUrl: String?
): Organization {
    val fullNumberInInternationalFormat = phoneNumber.takeIf { it.isNotBlank() }?.let {
        parsePhoneNumber(
            countryNameCode = countryNameCode,
            phoneNumber = it
        )
    }?.fullNumberInE164Format

    return Organization(
        name = name.trim(),
        email = email,
        logoUrl = logoImageUrl,
        phoneNumber = fullNumberInInternationalFormat,
        address = address,
        registrationNo = registrationNo.takeIf { it.isNotBlank() },
        description = description.takeIf { it.isNotBlank() },
        website = website.takeIf { it.isNotBlank() },
        socialLinks = listOf(facebookLink),
        userIds = listOf(userId)
    )
}

fun Organization.updated(
    request: CreateOrUpdateOrganizationRequest,
    logoImageUrl: String?,
    oldEmail: String,
    oldEmailVerified: Boolean,
    oldPhoneNumber: String?,
    oldPhoneNumberVerified: Boolean
): Organization {
    val newPhoneNumber = request.phoneNumber.takeIf { it.isNotBlank() }?.let {
        parsePhoneNumber(
            countryNameCode = request.countryNameCode,
            phoneNumber = it
        )
    }?.fullNumberInE164Format
    return this.copy(
        updatedAt = Clock.System.now(),
        logoUrl = logoImageUrl,
        name = request.name.trim(),
        email = request.email,
        emailVerified = if (oldEmail != request.email) false else oldEmailVerified,
        phoneNumber = newPhoneNumber,
        phoneNumberVerified = if (oldPhoneNumber != newPhoneNumber) false else oldPhoneNumberVerified,
        address = request.address,
        registrationNo = request.registrationNo.takeIf { it.isNotBlank() },
        description = request.description.takeIf { it.isNotBlank() },
        website = request.website.takeIf { it.isNotBlank() },
        socialLinks = listOf(request.facebookLink)
    )
}

fun Organization.toApiResponse(phoneNumber: PhoneNumber?): OrganizationResponse {
    return OrganizationResponse(
        address = address,
        createdAt = createdAt,
        description = description,
        email = email,
        emailVerified = emailVerified,
        id = id.toHexString(),
        logoUrl = logoUrl,
        name = name,
        phoneNumber = phoneNumber,
        phoneNumberVerified = phoneNumberVerified,
        registrationNo = registrationNo,
        socialLinks = socialLinks,
        website = website
    )
}