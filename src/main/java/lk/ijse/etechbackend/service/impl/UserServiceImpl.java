package lk.ijse.etechbackend.service.impl;

import lk.ijse.etechbackend.dto.*;
import lk.ijse.etechbackend.entity.Branch;
import lk.ijse.etechbackend.entity.User;
import lk.ijse.etechbackend.enumiration.Status;
import lk.ijse.etechbackend.enumiration.UserRole;
import lk.ijse.etechbackend.exception.BadRequestException;
import lk.ijse.etechbackend.exception.ForbiddenException;
import lk.ijse.etechbackend.exception.ResourceNotFoundException;
import lk.ijse.etechbackend.repository.BranchRepository;
import lk.ijse.etechbackend.repository.UserRepository;
import lk.ijse.etechbackend.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final BranchRepository branchRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public List<UserDTO> getAllEmployees(String currentUsername, UserRole roleFilter, String branchFilter,
            String search) {
        User currentUser = getCurrentUserEntity(currentUsername);
        UserRole currentRole = currentUser.getRole();

        if (currentRole != UserRole.SUPERADMIN && currentRole != UserRole.ADMIN) {
            throw new ForbiddenException("Access denied: Only SUPERADMIN and ADMIN can view the user directory");
        }

        boolean excludeSuperAdmin = (currentRole == UserRole.ADMIN);
        List<User> users = userRepository.filterEmployeeUsers(excludeSuperAdmin, roleFilter, branchFilter, search);

        return getUserDTOS(currentUser, currentRole, users);
    }

    @Override
    public List<UserDTO> getAllUsers(String currentUsername, UserRole roleFilter, String branchFilter, String search) {
        User currentUser = getCurrentUserEntity(currentUsername);
        UserRole currentRole = currentUser.getRole();

        if (currentRole != UserRole.SUPERADMIN && currentRole != UserRole.ADMIN) {
            throw new ForbiddenException("Access denied: Only SUPERADMIN and ADMIN can view the user directory");
        }

        boolean excludeSuperAdmin = (currentRole == UserRole.ADMIN);
        List<User> users = userRepository.filterUsers(excludeSuperAdmin, roleFilter, branchFilter, search);

        return getUserDTOS(currentUser, currentRole, users);
    }

    @Override
    public PageResponseDTO<UserDTO> getFilteredUsers(String currentUsername, UserRole role, Status status, String branch, String userType, String search, int page, int size, String sortBy, String sortDir) {
        User currentUser = getCurrentUserEntity(currentUsername);
        UserRole currentRole = currentUser.getRole();

        if (currentRole != UserRole.SUPERADMIN && currentRole != UserRole.ADMIN) {
            throw new ForbiddenException("Access denied: Only SUPERADMIN and ADMIN can view the user directory");
        }

        boolean excludeSuperAdmin = (currentRole == UserRole.ADMIN);
        Sort.Direction direction = "asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        String sortProperty = (sortBy != null && !sortBy.isBlank()) ? sortBy : "id";
        if ("name".equalsIgnoreCase(sortProperty)) {
            sortProperty = "name";
        } else if ("username".equalsIgnoreCase(sortProperty)) {
            sortProperty = "username";
        } else if ("email".equalsIgnoreCase(sortProperty)) {
            sortProperty = "email";
        } else if ("createdAt".equalsIgnoreCase(sortProperty) || "date".equalsIgnoreCase(sortProperty)) {
            sortProperty = "createdAt";
        }
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortProperty));

        String cleanSearch = (search != null && !search.isBlank()) ? search.trim().toLowerCase() : null;
        String cleanBranch = (branch != null && !branch.isBlank() && !"ALL".equalsIgnoreCase(branch.trim())) ? branch.trim() : null;
        String cleanUserType = (userType != null && !userType.isBlank() && !"all".equalsIgnoreCase(userType.trim())) ? userType.trim().toLowerCase() : null;

        Page<User> userPage = userRepository.filterUsersPaged(excludeSuperAdmin, role, status, cleanBranch, cleanUserType, cleanSearch, pageable);
        List<UserDTO> dtos = getUserDTOS(currentUser, currentRole, userPage.getContent());

        return PageResponseDTO.of(userPage, dtos);
    }

    @Override
    public List<UserDTO> getAllCustomers(String currentUsername, String search) {
        User currentUser = getCurrentUserEntity(currentUsername);
        UserRole currentRole = currentUser.getRole();

        if (currentRole != UserRole.SUPERADMIN && currentRole != UserRole.ADMIN) {
            throw new ForbiddenException("Access denied: Only SUPERADMIN and ADMIN can view the user directory");
        }

        boolean excludeSuperAdmin = (currentRole == UserRole.ADMIN);
        List<User> users = userRepository.filterCustomerUsers(search);

        return getUserDTOS(currentUser, currentRole, users);
    }

    @NonNull
    private List<UserDTO> getUserDTOS(User currentUser, UserRole currentRole, List<User> users) {
        return users.stream()
                .map(user -> {
                    boolean canManage;
                    if (currentRole == UserRole.SUPERADMIN) {
                        // Superadmin can manage everyone except the superadmin account itself
                        canManage = (user.getRole() != UserRole.SUPERADMIN
                                || Objects.equals(user.getId(), currentUser.getId()));
                    } else {
                        // Admin can manage STAFF and CUSTOMER, but NOT other Admins or Superadmin
                        canManage = (user.getRole() == UserRole.STAFF || user.getRole() == UserRole.CUSTOMER);
                    }
                    return mapToDTO(user, canManage);
                })
                .collect(Collectors.toList());
    }

    @Override
    public UserDTO getUserById(String currentUsername, Long id) {
        User currentUser = getCurrentUserEntity(currentUsername);
        User targetUser = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        // If requesting user is Admin and target is Superadmin, keep Superadmin
        // invisible
        if (currentUser.getRole() == UserRole.ADMIN && targetUser.getRole() == UserRole.SUPERADMIN) {
            throw new ResourceNotFoundException("User not found with id: " + id);
        }

        boolean canManage = false;
        if (currentUser.getRole() == UserRole.SUPERADMIN) {
            canManage = true;
        } else if (currentUser.getRole() == UserRole.ADMIN) {
            canManage = (targetUser.getRole() == UserRole.STAFF || targetUser.getRole() == UserRole.CUSTOMER);
        }

        return mapToDTO(targetUser, canManage);
    }

    @Override
    @Transactional
    public UserDTO createUser(String currentUsername, UserDTO request) {
        User currentUser = getCurrentUserEntity(currentUsername);

        // RBAC validation
        if (currentUser.getRole() == UserRole.ADMIN) {
            if (request.getRole() == UserRole.ADMIN || request.getRole() == UserRole.SUPERADMIN) {
                throw new ForbiddenException("Admins are not authorized to create Admin or Superadmin accounts");
            }
        } else if (currentUser.getRole() == UserRole.SUPERADMIN) {
            if (request.getRole() == UserRole.SUPERADMIN) {
                throw new BadRequestException("Only one unique Superadmin account is permitted in the system");
            }
        } else {
            throw new ForbiddenException("Access denied: You do not have permission to create user accounts");
        }

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BadRequestException("Username '" + request.getUsername() + "' is already taken");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email '" + request.getEmail() + "' is already registered");
        }

        Branch assignedBranch = null;
        if (request.getAssignedBranch() != null && !request.getAssignedBranch().isBlank()) {
            assignedBranch = branchRepository.findById(request.getAssignedBranch())
                    .orElseThrow(
                            () -> new BadRequestException("Branch not found with id: " + request.getAssignedBranch()));
        }

        User user = User.builder()
                .name(request.getName())
                .username(request.getUsername())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole())
                .assignedBranch(assignedBranch)
                .build();

        User savedUser = userRepository.save(user);
        log.info("Created user {} ({}) by {}", savedUser.getUsername(), savedUser.getRole(), currentUsername);

        return mapToDTO(savedUser, true);
    }

    @Override
    @Transactional
    public UserDTO updateUser(String currentUsername, Long id, UserDTO request) {
        User currentUser = getCurrentUserEntity(currentUsername);
        User targetUser = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        // RBAC validation
        if (targetUser.getRole() == UserRole.SUPERADMIN) {
            if (currentUser.getRole() != UserRole.SUPERADMIN) {
                throw new ForbiddenException("Only Superadmin can modify the Superadmin account");
            }
            if (request.getRole() != UserRole.SUPERADMIN) {
                throw new BadRequestException("Superadmin role cannot be downgraded or modified");
            }
        } else if (currentUser.getRole() == UserRole.ADMIN) {
            if (targetUser.getRole() == UserRole.ADMIN) {
                throw new ForbiddenException("Admins cannot modify other Admin accounts");
            }
            if (request.getRole() == UserRole.ADMIN || request.getRole() == UserRole.SUPERADMIN) {
                throw new ForbiddenException("Admins cannot assign Admin or Superadmin roles");
            }
        }

        // Validate unique username if changed
        if (!targetUser.getUsername().equalsIgnoreCase(request.getUsername()) &&
                userRepository.existsByUsername(request.getUsername())) {
            throw new BadRequestException("Username '" + request.getUsername() + "' is already taken");
        }

        // Validate unique email if changed
        if (!targetUser.getEmail().equalsIgnoreCase(request.getEmail()) &&
                userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email '" + request.getEmail() + "' is already registered");
        }

        Branch assignedBranch = null;
        if (request.getAssignedBranch() != null && !request.getAssignedBranch().isBlank()) {
            assignedBranch = branchRepository.findById(request.getAssignedBranch())
                    .orElseThrow(
                            () -> new BadRequestException("Branch not found with id: " + request.getAssignedBranch()));
        }

        targetUser.setName(request.getName());
        targetUser.setUsername(request.getUsername());
        targetUser.setEmail(request.getEmail());
        targetUser.setRole(request.getRole());
        targetUser.setAssignedBranch(assignedBranch);

        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            if (request.getPassword().length() < 6) {
                throw new BadRequestException("Password must be at least 6 characters");
            }
            targetUser.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        }

        User updatedUser = userRepository.save(targetUser);
        log.info("Updated user ID: {} by {}", id, currentUsername);

        return mapToDTO(updatedUser, true);
    }

    @Override
    @Transactional
    public UserDTO changeUserRole(String currentUsername, Long id, UserDTO request) {
        User currentUser = getCurrentUserEntity(currentUsername);
        User targetUser = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        // Immutable Superadmin check
        if (targetUser.getRole() == UserRole.SUPERADMIN) {
            throw new BadRequestException("Superadmin role is immutable and cannot be changed");
        }

        // Admin restrictions
        if (currentUser.getRole() == UserRole.ADMIN) {
            if (targetUser.getRole() == UserRole.ADMIN) {
                throw new ForbiddenException("Admins cannot change roles of other Admin accounts");
            }
            if (request.getRole() == UserRole.ADMIN || request.getRole() == UserRole.SUPERADMIN) {
                throw new ForbiddenException("Admins cannot promote users to Admin or Superadmin");
            }
        }

        Branch assignedBranch = null;
        if (request.getAssignedBranch() != null && !request.getAssignedBranch().isBlank()) {
            assignedBranch = branchRepository.findById(request.getAssignedBranch())
                    .orElseThrow(
                            () -> new BadRequestException("Branch not found with id: " + request.getAssignedBranch()));
        }

        targetUser.setRole(request.getRole());
        targetUser.setAssignedBranch(assignedBranch);

        User updatedUser = userRepository.save(targetUser);
        log.info("Changed role for user ID: {} to {} by {}", id, request.getRole(), currentUsername);

        return mapToDTO(updatedUser, true);
    }

    @Override
    @Transactional
    public void deleteUser(String currentUsername, Long id) {
        User currentUser = getCurrentUserEntity(currentUsername);
        User targetUser = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        // Superadmin account can NEVER be deleted
        if (targetUser.getRole() == UserRole.SUPERADMIN) {
            throw new BadRequestException("The primary Superadmin account cannot be deleted");
        }

        // Admin cannot delete other Admins
        if (currentUser.getRole() == UserRole.ADMIN && targetUser.getRole() == UserRole.ADMIN) {
            throw new ForbiddenException("Admins are not authorized to delete other Admin accounts");
        }

        targetUser.setStatus(Status.DELETED);

        userRepository.save(targetUser);
        log.info("Deleted user ID: {} by {}", id, currentUsername);
    }

    @Override
    @Transactional
    public UserDTO updateProfile(String currentUsername, UserDTO request) {
        User user = getCurrentUserEntity(currentUsername);

        if (!user.getUsername().equalsIgnoreCase(request.getUsername()) &&
                userRepository.existsByUsername(request.getUsername())) {
            throw new BadRequestException("Username '" + request.getUsername() + "' is already taken");
        }

        if (!user.getEmail().equalsIgnoreCase(request.getEmail()) &&
                userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email '" + request.getEmail() + "' is already registered");
        }

        user.setName(request.getName());
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());

        User updatedUser = userRepository.save(user);
        log.info("User {} updated their personal profile", currentUsername);

        return mapToDTO(updatedUser, null);
    }

    @Override
    @Transactional
    public void changePassword(String currentUsername, UserDTO request) {
        User user = getCurrentUserEntity(currentUsername);

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new BadRequestException("Current password does not match");
        }

        if (passwordEncoder.matches(request.getNewPassword(), user.getPasswordHash())) {
            throw new BadRequestException("New password must be different from current password");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        log.info("User {} successfully changed their password", currentUsername);
    }

    @Override
    public List<String> getRoles() {
        return List.of(UserRole.SUPERADMIN.name(), UserRole.ADMIN.name(), UserRole.STAFF.name(),
                UserRole.CUSTOMER.name());
    }

    @Override
    @Transactional
    public UserDTO updateUserStatus(String currentUsername, Long id, UserDTO request) {
        User currentUser = getCurrentUserEntity(currentUsername);
        User targetUser = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        if (targetUser.getRole() == UserRole.SUPERADMIN) {
            throw new BadRequestException("Superadmin account status cannot be modified");
        }

        if (currentUser.getRole() == UserRole.ADMIN && targetUser.getRole() == UserRole.ADMIN) {
            throw new ForbiddenException("Admins cannot modify the status of other Admin accounts");
        }

        if (request.getStatus() != null) {
            targetUser.setStatus(request.getStatus());
        }

        User saved = userRepository.save(targetUser);
        log.info("User {} updated status of user ID {} to {}", currentUsername, id, saved.getStatus());
        return mapToDTO(saved, true);
    }

    private User getCurrentUserEntity(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Current authenticated user not found: " + username));
    }

    private UserDTO mapToDTO(User user, Boolean canManage) {
        return UserDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole())
                .status(user.getStatus())
                .assignedBranch(user.getAssignedBranch() != null ? user.getAssignedBranch().getId() : null)
                .canManage(canManage)
                .createdAt(user.getCreatedAt())
                .build();
    }
}
