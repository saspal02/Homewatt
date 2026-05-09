package com.homewatt.user_service.service;

import com.homewatt.user_service.dto.UserDto;
import com.homewatt.user_service.entity.User;
import com.homewatt.user_service.exception.UserNotFoundException;
import com.homewatt.user_service.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UserDto createUser(UserDto input) {

        User createdUser = User.builder()
                .name(input.name())
                .surname(input.surname())
                .email(input.email())
                .address(input.address())
                .alerting(input.alerting())
                .energyAlertingThreshold(input.energyAlertingThreshold())
                .build();

        User saved = userRepository.save(createdUser);

        return mapToDto(saved);
    }

    public UserDto getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() ->
                        new UserNotFoundException("User not found with id " + id));

        return mapToDto(user);
    }

    public UserDto updateUser(Long id, UserDto userDto) {

        User user = userRepository.findById(id)
                .orElseThrow(() ->
                        new UserNotFoundException("User not found with id " + id));

        user.setName(userDto.name());
        user.setSurname(userDto.surname());
        user.setEmail(userDto.email());
        user.setAddress(userDto.address());
        user.setAlerting(userDto.alerting());
        user.setEnergyAlertingThreshold(userDto.energyAlertingThreshold());

        User updatedUser = userRepository.save(user);

        return mapToDto(updatedUser);
    }

    public void deleteUser(Long id) {

        User user = userRepository.findById(id)
                .orElseThrow(() ->
                        new UserNotFoundException("User not found with id " + id));

        userRepository.delete(user);
    }

    private UserDto mapToDto(User user) {
        return new UserDto(
                user.getId(),
                user.getName(),
                user.getSurname(),
                user.getEmail(),
                user.getAddress(),
                user.isAlerting(),
                user.getEnergyAlertingThreshold()
        );
    }
}
