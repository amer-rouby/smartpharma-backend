package com.smartpharma.service;

import com.smartpharma.dto.request.UserRequest;
import com.smartpharma.dto.response.UserResponse;

import java.util.List;

public interface UserService {

    List<UserResponse> getAllUsers(Long pharmacyId);

    UserResponse getUser(Long id, Long pharmacyId);

    UserResponse createUser(UserRequest request);

    UserResponse updateUser(Long id, UserRequest request, Long pharmacyId);

    void deleteUser(Long id, Long pharmacyId);

    List<UserResponse> searchUsers(Long pharmacyId, String query);

    Long getUsersCount(Long pharmacyId);

    List<UserResponse> getActiveUsers(Long pharmacyId);
}