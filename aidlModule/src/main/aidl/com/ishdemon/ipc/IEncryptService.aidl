package com.ishdemon.ipc;

interface IEncryptService {
    byte[] getPublicKey();
    byte[] processData(in byte[] encryptedData, in byte[] clientPublicKeyBytes);
}