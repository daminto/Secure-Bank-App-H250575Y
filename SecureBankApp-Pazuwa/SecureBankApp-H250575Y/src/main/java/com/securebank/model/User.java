package com.securebank.model;

/** A registered user. Only a salted PBKDF2 hash of the password is ever stored. */
public record User(String username, byte[] salt, int iterations, byte[] passwordHash) { }
