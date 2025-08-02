# Secure IPC with Encryption

This project demonstrates secure inter-process communication (IPC) between two Android applications — **ClientApp** and **ServerApp** — using AIDL for request–response and Messenger for one-way push messages.  
The communication is encrypted using asymmetric RSA keys.

---

## Design Document

### Overview
This project implements secure inter-process communication (IPC) between two Android applications - ClientApp and ServerApp - using AIDL for request–response and Messenger for one-way push messages. The communication is encrypted using asymmetric RSA keys.

### Goals
- Allow two separate Android apps to exchange sensitive data securely.
- Ensure data confidentiality and integrity.
- Demonstrate bidirectional communication:
    - AIDL -> request–response
    - Messenger -> asynchronous push notifications

### Key Requirements
1. Asymmetric Encryption for confidentiality.
2. Public Key Exchange between client and server.
3. AIDL Interface for secure request–response.
4. Messenger Interface for one-way server push.
5. Shared Common Module (`:aidlModule`) for:
    - AIDL file
    - Crypto utilities
6. Dependency Injection using Hilt.
7. MVVM architecture in ClientApp.

### High-Level Flow
1. Client binds to ServerApp's AIDL service and Messenger service.
2. Server provides its public key.
3. Client generates its own key pair, sends its public key with encrypted data.
4. Server decrypts request with its private key, processes it, and encrypts the response with the client's public key.
5. Client decrypts the response with its private key.
6. Server sends a delayed push message via Messenger.

---

## Architecture Document

I have used MVVM architecture with flows 

Activity (UI) <--observes--> ViewModel <--Hilt injects--> SecureClient & PushClient (IPC layer)


### Module Structure

SecureIPCProject/
│
├── settings.gradle                  # Includes ClientApp, ServerApp, aidlModule
├── build.gradle.kts                  # Root Gradle file with plugin aliases
├── gradle/
│   └── libs.versions.toml            # Version catalog (AGP 8.5.2, Hilt, etc.)
├── README.md
│
├── aidlModule/                       # Shared library module
│   ├── build.gradle.kts              # Hilt + KSP enabled Android library
│   └── src/main/
│       ├── aidl/com/ishdemon/ipc/IEncryptService.aidl
│       └── java/com/ishdemon/common/CryptoUtils.kt
│
├── ClientApp/                        # Client application
│   ├── build.gradle.kts
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/ishdemon/clientapp/
│       │   ├── ClientApp.kt
│       │   ├── ipc/
│       │   │   ├── SecureClient.kt
│       │   │   └── PushClient.kt
│       │   ├── viewmodel/
│       │   │   └── SecureViewModel.kt
│       │   └── ui/
│       │       └── MainActivity.kt
│       └── res/layout/activity_main.xml
│
└── ServerApp/                        # Server application (secure service)
├── build.gradle.kts
└── src/main/
├── AndroidManifest.xml
└── java/com/ishdemon/serverapp/
├── ServerApp.kt
├── SecureService.kt
└── di/                    # Optional: Hilt modules if needed

