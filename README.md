# Auth KMP

A Kotlin Multiplatform project for authentication and user management in mobile and web applications.

## Overview

Auth KMP provides a complete authentication solution for Kotlin Multiplatform projects. It offers two main artifacts:

1. **auth** - Core authentication module

2. **auth-ui** - UI components for authentication flows

## Installation

Add the Auth KMP dependencies to your project:

```kotlin
// In your build.gradle.kts
dependencies {
    // Core auth module
    implementation("com.vardansoft:auth:x.y.z")
    
    // Optional UI components
    implementation("com.vardansoft:auth-ui:x.y.z")
}
```

## Setup

### Core Auth Module

1. **Initialize Koin modules**

   Include the auth module when initializing Koin:

   ```kotlin
   // In your application initialization
   // Set your auth server base URL once (optional; can also be set later)
   VardanSoftAuth.init(baseUrl = "https://auth.yourdomain.com")

   startKoin {
       modules(
           // Your other modules
           authSharedModule
       )
   }
   ```

2. **Configure Ktor client with authentication**

   Set up your Ktor client with authentication:

   ```kotlin
   val httpClient = HttpClient {
       install(Auth) {
           // Pass your OAuth2 public client ID
           applyVardanSoftBearer(this@single, YOUR_CLIENT_ID)
       }
       // Other configurations
   }
   ```

### UI Module

1. **Initialize Koin modules**

   Include both auth and UI modules:

   ```kotlin
   startKoin {
       modules(
           // Your other modules
           getAuthModules() // This includes both auth and UI modules
       )
   }
   ```

2. **Set up user information provider**

   Wrap your UI with the user information provider:

   ```kotlin
   ProvideUserInfo {
       // Your app content
       val user = LocalUserInfo.current // Access user info anywhere in this scope
   }
   ```


### UI Components

#### OTP Screen

```kotlin
composable<Screen.Otp> {
    val vm = koinViewModel<OtpViewModel>()
    val state = vm.state.collectAsState().value
    OtpScreen(
        state = state,
        onEvent = vm::onEvent,
        popBackStack = navController::popBackStack,
        uiEvents = vm.uiEvents
    )
}
```

#### Login

```kotlin
// Platform-specific credential retriever
val credentialRetriever = rememberCredentialRetriever(clientId = "your-client-id")

// Use in your login flow
LaunchedEffect(Unit) {
    val credentialResult = credentialRetriever.getCredential()
    credentialResult.onSuccess { credential ->
        viewModel.login(credential)
    }
}
```

## API Reference

### UI Module

- **OtpScreen** - UI component for OTP verification
- **CredentialRetriever** - Platform-specific interface for retrieving credentials
- **ProvideUserInfo** - Composable for providing user information to the UI
- **LocalUserInfo** - CompositionLocal for accessing user information

## Platform Support

- Android
- iOS
- JVM (Desktop)
- Web (JS)
