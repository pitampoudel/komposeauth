package pitampoudel.komposeauth

import io.sentry.Sentry
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
import pitampoudel.core.data.ErrorSnapshotResponse
import pitampoudel.core.domain.now
import javax.security.auth.login.AccountLockedException
import javax.security.auth.login.AccountNotFoundException
import kotlin.time.ExperimentalTime

@ControllerAdvice
@RequestMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
@OptIn(ExperimentalTime::class)
class GlobalExceptionHandler {

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleJsonParseException(
        ex: HttpMessageNotReadableException,
        request: WebRequest
    ): ResponseEntity<ErrorSnapshotResponse> {
        return ResponseEntity(
            ErrorSnapshotResponse(
                timestamp = now(),
                message = ex.rootCause?.message ?: ex.message
                ?: "Invalid request body: The provided JSON is malformed or invalid.",
                path = request.getDescription(false).removePrefix("uri=")
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
                timestamp = now(),
                message = ex.message ?: "Method not supported",
                path = request.getDescription(false).removePrefix("uri=")
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
                timestamp = now(),
                message = "A resource with the same unique identifier already exists.",
                path = request.getDescription(false).removePrefix("uri=")
            ),
            HttpStatus.CONFLICT
        )
    }

    @ExceptionHandler(DataIntegrityViolationException::class)
    fun handleDataIntegrityViolation(
        ex: DataIntegrityViolationException,
        request: WebRequest
    ): ResponseEntity<ErrorSnapshotResponse> {
        return ResponseEntity(
            ErrorSnapshotResponse(
                timestamp = now(),
                message = ex.rootCause?.message ?: ex.message ?: "Data integrity violation",
                path = request.getDescription(false).removePrefix("uri=")
            ),
            HttpStatus.CONFLICT
        )
    }

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgumentException(
        ex: IllegalArgumentException,
        request: WebRequest
    ): ResponseEntity<ErrorSnapshotResponse> {
        return ResponseEntity(
            ErrorSnapshotResponse(
                timestamp = now(),
                message = ex.message ?: "Invalid argument provided.",
                path = request.getDescription(false).removePrefix("uri=")
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
                timestamp = now(),
                message = ex.message,
                path = request.getDescription(false).removePrefix("uri=")
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
                timestamp = now(),
                message = ex.message,
                path = request.getDescription(false).removePrefix("uri=")
            ),
            HttpStatus.NOT_FOUND
        )
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationExceptions(
        ex: MethodArgumentNotValidException,
        request: WebRequest
    ): ResponseEntity<ErrorSnapshotResponse> {
        val errors = ex.bindingResult.fieldErrors.joinToString(", ") {
            it.defaultMessage ?: "Validation failed for field ${it.field}"
        }
        return ResponseEntity(
            ErrorSnapshotResponse(
                timestamp = now(),
                message = "Validation failed: $errors",
                path = request.getDescription(false).removePrefix("uri=")
            ),
            HttpStatus.BAD_REQUEST
        )
    }

    @ExceptionHandler(BadRequestException::class)
    fun handleApacheCoyoteBadRequestException(
        ex: BadRequestException,
        request: WebRequest
    ): ResponseEntity<ErrorSnapshotResponse> {
        return ResponseEntity(
            ErrorSnapshotResponse(
                timestamp = now(),
                message = ex.message,
                path = request.getDescription(false).removePrefix("uri=")
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
                timestamp = now(),
                message = ex.message ?: "You do not have permission to perform this action.",
                path = request.getDescription(false).removePrefix("uri=")
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
                timestamp = now(),
                message = ex.message ?: "This operation is not supported.",
                path = request.getDescription(false).removePrefix("uri=")
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
                timestamp = now(),
                message = ex.message ?: "Unsupported media type",
                path = request.getDescription(false).removePrefix("uri=")
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
        return ResponseEntity(
            ErrorSnapshotResponse(
                timestamp = now(),
                message = ex.message ?: "An unexpected error occurred",
                path = request.getDescription(false).removePrefix("uri=")
            ), HttpStatus.INTERNAL_SERVER_ERROR
        )
    }
}


