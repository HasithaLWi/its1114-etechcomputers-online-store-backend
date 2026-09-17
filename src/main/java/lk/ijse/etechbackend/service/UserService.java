package lk.ijse.etechbackend.service;

import lk.ijse.etechbackend.dto.*;
import lk.ijse.etechbackend.enumiration.UserRole;

import java.util.List;

public interface UserService {

    List<UserDTO> getAllEmployees(String currentUsername, UserRole role, String branch, String search);

    List<UserDTO> getAllUsers(String currentUsername, UserRole roleFilter, String branchFilter, String search);

    PageResponseDTO<UserDTO> getFilteredUsers(String currentUsername, UserRole role, lk.ijse.etechbackend.enumiration.Status status, String branch, String userType, String search, int page, int size, String sortBy, String sortDir);

    List<UserDTO> getAllCustomers(String currentUsername, String search);

    UserDTO getUserById(String currentUsername, Long id);

    UserDTO createUser(String currentUsername, UserDTO request);

    UserDTO updateUser(String currentUsername, Long id, UserDTO request);

    UserDTO changeUserRole(String currentUsername, Long id, UserDTO request);

    void deleteUser(String currentUsername, Long id);

    UserDTO updateProfile(String currentUsername, UserDTO request);

    void changePassword(String currentUsername, UserDTO request);

    List<String> getRoles();

    UserDTO updateUserStatus(String currentUsername, Long id, UserDTO request);
}

