package com.smartpharma.service;

import com.smartpharma.dto.request.LoginRequest;
import com.smartpharma.dto.request.RegisterRequest;
import com.smartpharma.dto.response.AuthResponse;

public interface AuthenticationService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    AuthResponse refreshToken(String refreshToken);
}