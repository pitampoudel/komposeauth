package pitampoudel.komposeauth

import co.touchlab.kermit.Logger
import io.sentry.Sentry
import kotlinx.serialization.Serializable
import org.apache.coyote.BadRequestException
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.dao.DuplicateKeyException
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.web.HttpMediaTypeNotSupportedException
import org.springframework.web.HttpRequestMethodNotSupportedException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.context.request.WebRequest
import org.springframework.web.servlet.resource.NoResourceFoundException
import pitampoudel.core.domain.now
import javax.security.auth.login.AccountLockedException
import javax.security.auth.login.AccountNotFoundException
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@Serializable
data class ErrorSnapshotResponse(
    val timestamp: Instant = now(),
    val message: String?,
    val path: String
)

@ControllerAdvice
@RequestMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
@OptIn(ExperimentalTime::class)
class GlobalExceptionHandler {

    private val logger = Logger.withTag("GlobalExceptionHandler")

    fun WebRequest.path() = getDescription(false).removePrefix("uri=")


    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleJsonParseException(
        ex: HttpMessageNotReadableException,
        request: WebRequest
    ): ResponseEntity<ErrorSnapshotResponse> {
        logger.e(ex) { "JSON parse error at ${request.path()}" }
        return ResponseEntity(
            ErrorSnapshotResponse(
                message = "Invalid request body: The provided JSON is malformed or invalid.",
                path = request.path()
            ),
            HttpStatus.BAD_REQUEST
        )
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException::class)
    fun handleMethodNotSupported(
        ex: HttpRequestMethodNotSupportedException,
        request: WebRequest
    ): ResponseEntity<ErrorSnapshotResponse> {
        return ResponseEntity(
            ErrorSnapshotResponse(
                message = ex.message ?: "Method not supported",
                path = request.path()
            ),
            HttpStatus.METHOD_NOT_ALLOWED
        )
    }

    @ExceptionHandler(DuplicateKeyException::class)
    fun handleDuplicateKeyException(
        ex: DuplicateKeyException,
        request: WebRequest
    ): ResponseEntity<ErrorSnapshotResponse> {
        return ResponseEntity(
            ErrorSnapshotResponse(
                message = "A resource with the same unique identifier already exists.",
                path = request.path()
            ),
            HttpStatus.CONFLICT
        )
    }

    @ExceptionHandler(DataIntegrityViolationException::class)
    fun handleDataIntegrityViolation(
        ex: DataIntegrityViolationException,
        request: WebRequest
    ): ResponseEntity<ErrorSnapshotResponse> {
        logger.e(ex) { "Data integrity violation at ${request.path()}" }
        return ResponseEntity(
            ErrorSnapshotResponse(
                message = "Data integrity violation",
                path = request.path()
            ),
            HttpStatus.CONFLICT
        )
    }

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgumentException(
        ex: IllegalArgumentException,
        request: WebRequest
    ): ResponseEntity<ErrorSnapshotResponse> {
        logger.e(ex) { "Illegal argument at ${request.path()}" }
        return ResponseEntity(
            ErrorSnapshotResponse(
                message = ex.message ?: "Invalid argument provided.",
                path = request.path()
            ),
            HttpStatus.BAD_REQUEST
        )
    }

    @ExceptionHandler(UsernameNotFoundException::class)
    fun handleUsernameNotFoundException(
        ex: UsernameNotFoundException,
        request: WebRequest
    ): ResponseEntity<ErrorSnapshotResponse> {
        return ResponseEntity(
            ErrorSnapshotResponse(
                message = ex.message,
                path = request.path()
            ),
            HttpStatus.NOT_FOUND
        )
    }

    @ExceptionHandler(AccountNotFoundException::class, NoResourceFoundException::class)
    fun handleAccountNotFoundException(
        ex: Exception,
        request: WebRequest
    ): ResponseEntity<ErrorSnapshotResponse> {
        return ResponseEntity(
            ErrorSnapshotResponse(
                message = ex.message,
                path = request.path()
            ),
            HttpStatus.NOT_FOUND
        )
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationExceptions(
        ex: MethodArgumentNotValidException,
        request: WebRequest
    ): ResponseEntity<ErrorSnapshotResponse> {
        logger.e(ex) { "Validation error at ${request.path()}" }
        val errors = ex.bindingResult.fieldErrors.joinToString(", ") {
            it.defaultMessage ?: "Validation failed for field ${it.field}"
        }
        return ResponseEntity(
            ErrorSnapshotResponse(
                message = "Validation failed: $errors",
                path = request.path()
            ),
            HttpStatus.BAD_REQUEST
        )
    }

    @ExceptionHandler(BadRequestException::class)
    fun handleApacheCoyoteBadRequestException(
        ex: BadRequestException,
        request: WebRequest
    ): ResponseEntity<ErrorSnapshotResponse> {
        logger.e(ex) { "Bad request at ${request.path()}" }
        return ResponseEntity(
            ErrorSnapshotResponse(
                message = ex.message,
                path = request.path()
            ),
            HttpStatus.BAD_REQUEST
        )
    }

    @ExceptionHandler(AccessDeniedException::class, AccountLockedException::class)
    fun handleAccessDeniedException(
        ex: Exception,
        request: WebRequest
    ): ResponseEntity<ErrorSnapshotResponse> {
        return ResponseEntity(
            ErrorSnapshotResponse(
                message = ex.message ?: "You do not have permission to perform this action.",
                path = request.path()
            ),
            HttpStatus.FORBIDDEN
        )
    }

    @ExceptionHandler(UnsupportedOperationException::class)
    fun handleUnsupportedOperationException(
        ex: UnsupportedOperationException,
        request: WebRequest
    ): ResponseEntity<ErrorSnapshotResponse> {
        return ResponseEntity(
            ErrorSnapshotResponse(
                message = ex.message ?: "This operation is not supported.",
                path = request.path()
            ),
            HttpStatus.NOT_IMPLEMENTED
        )
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException::class)
    fun handleHttpMediaTypeNotSupportedException(
        ex: HttpMediaTypeNotSupportedException,
        request: WebRequest
    ): ResponseEntity<ErrorSnapshotResponse> {
        return ResponseEntity(
            ErrorSnapshotResponse(
                message = ex.message ?: "Unsupported media type",
                path = request.path()
            ),
            HttpStatus.UNSUPPORTED_MEDIA_TYPE
        )
    }

    @ExceptionHandler(Exception::class)
    fun handleGlobalException(
        ex: Exception,
        request: WebRequest
    ): ResponseEntity<ErrorSnapshotResponse> {
        Sentry.captureException(ex)
        logger.e(ex) { "Unexpected error at ${request.path()}" }
        return ResponseEntity(
            ErrorSnapshotResponse(
                message = "An unexpected error occurred",
                path = request.path()
            ), HttpStatus.INTERNAL_SERVER_ERROR
        )
    }
}


