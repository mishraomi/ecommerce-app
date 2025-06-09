package com.peerislands.ecommerce.service;

import com.peerislands.ecommerce.entity.User;
import com.peerislands.ecommerce.exception.UserNotFoundException;
import com.peerislands.ecommerce.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    private User user;
    private User updatedUser;

    @BeforeEach
    void setUp() {
        // Setup test user
        user = new User();
        user.setId("USER-001");
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setEmail("john.doe@example.com");

        // Setup updated user
        updatedUser = new User();
        updatedUser.setId("USER-001");
        updatedUser.setFirstName("John");
        updatedUser.setLastName("Smith");
        updatedUser.setEmail("john.smith@example.com");
    }

    @Test
    void createUser_WithValidData_ShouldCreateUser() {
        // Arrange
        when(userRepository.save(any(User.class))).thenReturn(user);

        // Act
        User result = userService.createUser(user);

        // Assert
        assertNotNull(result);
        assertEquals("USER-001", result.getId());
        assertEquals("John", result.getFirstName());
        assertEquals("Doe", result.getLastName());
        assertEquals("john.doe@example.com", result.getEmail());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void getUserById_WhenUserExists_ShouldReturnUser() {
        // Arrange
        when(userRepository.findById("USER-001")).thenReturn(Optional.of(user));

        // Act
        User result = userService.getUserById("USER-001");

        // Assert
        assertNotNull(result);
        assertEquals("USER-001", result.getId());
        assertEquals("John", result.getFirstName());
        assertEquals("Doe", result.getLastName());
        assertEquals("john.doe@example.com", result.getEmail());
        verify(userRepository).findById("USER-001");
    }

    @Test
    void getUserById_WhenUserDoesNotExist_ShouldThrowException() {
        // Arrange
        when(userRepository.findById("NON-EXISTENT")).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(UserNotFoundException.class, () -> 
            userService.getUserById("NON-EXISTENT")
        );
        verify(userRepository).findById("NON-EXISTENT");
    }

    @Test
    void getAllUsers_ShouldReturnAllUsers() {
        // Arrange
        List<User> users = new ArrayList<>();
        users.add(user);
        when(userRepository.findAll()).thenReturn(users);

        // Act
        List<User> result = userService.getAllUsers();

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("USER-001", result.get(0).getId());
        verify(userRepository).findAll();
    }

    @Test
    void updateUser_WhenUserExists_ShouldUpdateUser() {
        // Arrange
        when(userRepository.findById("USER-001")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(updatedUser);

        // Act
        User result = userService.updateUser("USER-001", updatedUser);

        // Assert
        assertNotNull(result);
        assertEquals("USER-001", result.getId());
        assertEquals("John", result.getFirstName());
        assertEquals("Smith", result.getLastName());
        assertEquals("john.smith@example.com", result.getEmail());
        verify(userRepository).findById("USER-001");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void updateUser_WhenUserDoesNotExist_ShouldThrowException() {
        // Arrange
        when(userRepository.findById("NON-EXISTENT")).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(UserNotFoundException.class, () -> 
            userService.updateUser("NON-EXISTENT", updatedUser)
        );
        verify(userRepository).findById("NON-EXISTENT");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void deleteUser_WhenUserExists_ShouldDeleteUser() {
        // Arrange
        when(userRepository.findById("USER-001")).thenReturn(Optional.of(user));
        doNothing().when(userRepository).delete(any(User.class));

        // Act
        userService.deleteUser("USER-001");

        // Assert
        verify(userRepository).findById("USER-001");
        verify(userRepository).delete(user);
    }

    @Test
    void deleteUser_WhenUserDoesNotExist_ShouldThrowException() {
        // Arrange
        when(userRepository.findById("NON-EXISTENT")).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(UserNotFoundException.class, () -> 
            userService.deleteUser("NON-EXISTENT")
        );
        verify(userRepository).findById("NON-EXISTENT");
        verify(userRepository, never()).delete(any(User.class));
    }

    @Test
    void getUserByEmail_WhenUserExists_ShouldReturnUser() {
        // Arrange
        List<User> users = new ArrayList<>();
        users.add(user);
        when(userRepository.findAll()).thenReturn(users);

        // Act
        User result = userService.getUserByEmail("john.doe@example.com");

        // Assert
        assertNotNull(result);
        assertEquals("USER-001", result.getId());
        assertEquals("john.doe@example.com", result.getEmail());
        verify(userRepository).findAll();
    }

    @Test
    void getUserByEmail_WhenUserDoesNotExist_ShouldThrowException() {
        // Arrange
        List<User> users = new ArrayList<>();
        users.add(user);
        when(userRepository.findAll()).thenReturn(users);

        // Act & Assert
        assertThrows(UserNotFoundException.class, () -> 
            userService.getUserByEmail("nonexistent@example.com")
        );
        verify(userRepository).findAll();
    }
} 