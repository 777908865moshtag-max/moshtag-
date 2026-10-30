# Security Specification — Moshtaq Grafx AI Studio (`firestore.rules`)

## 1. Data Invariants
1. **Authentication Mandatory**: No unauthenticated read, list, create, update, or delete is permitted on any path (`request.auth != null`).
2. **Strict Tenant Isolation**: A project document under `/users/{userId}/projects/{projectId}` can only be read, listed, created, updated, or deleted when `request.auth.uid == userId` AND `incoming().userId == userId`.
3. **Path ID Hardening**: `userId` and `projectId` must match `^[a-zA-Z0-9_\-]+$` with length `<= 128`.
4. **Schema & Bounds Enforcement (`isValidStudioCloudProject`)**:
   - `userId`: string, matches `request.auth.uid`, size 1..128
   - `title`: string, size 1..200
   - `categoryTitleAr`: string, size 1..200
   - `processingModeAr`: string, size 1..200
   - `aspectRatioLabel`: string, size 1..40
   - `resolutionLabel`: string, size 1..40
   - `outputWidth`: int, `>= 1 && <= 16384`
   - `outputHeight`: int, `>= 1 && <= 16384`
   - `exportFormat`: string, in `['PNG', 'JPEG']`
   - `savedFilePath`: string, size 1..1024
   - `originalFileName`: string, size 1..500
   - `notesAr`: optional string, size `<= 2000`
   - `createdAt` & `updatedAt`: `is timestamp && <= request.time`
5. **Immutability on Update**: `userId` and `createdAt` cannot be modified after creation (`incoming().userId == existing().userId && incoming().createdAt == existing().createdAt`).

## 2. The "Dirty Dozen" Payloads
1. **Unauthenticated Read**: `unauthDb.collection("users/alice/projects").get()` -> Reject
2. **Cross-User Read**: Alice reading `/users/bob/projects/p1` -> Reject
3. **Cross-User Create (Identity Spoofing)**: Alice writing to `/users/alice/projects/p1` with `userId: "bob"` -> Reject
4. **Shadow Field Injection**: Writing `isVerified: true` or `admin: true` not in `hasOnly` allowlist -> Reject
5. **Missing Required Key**: Omitting `title` or `createdAt` on create -> Reject
6. **Oversized String (Denial of Wallet)**: `title` with 5,000 characters -> Reject
7. **Invalid Export Format Enum**: `exportFormat: "EXE"` -> Reject
8. **Invalid Dimensions**: `outputWidth: -10` or `outputWidth: "1024"` -> Reject
9. **Future Timestamp Spoofing**: `createdAt` set to future timestamp `> request.time` -> Reject
10. **Immutable Field Mutation on Update**: Changing `createdAt` or `userId` during update -> Reject
11. **Value Poisoning on Update**: Updating `title` to an integer `12345` -> Reject
12. **Poisoned Document ID**: Creating document with ID containing spaces or special characters -> Reject
