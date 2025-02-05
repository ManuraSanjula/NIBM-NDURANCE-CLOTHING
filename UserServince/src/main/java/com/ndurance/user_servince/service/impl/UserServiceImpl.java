package com.ndurance.user_servince.service.impl;

import com.ndurance.user_servince.entity.AddressEntity;
import com.ndurance.user_servince.exceptions.UserServiceException;
import com.ndurance.user_servince.repository.*;
import com.ndurance.user_servince.security.UserPrincipal;
import com.ndurance.user_servince.shared.Utils;
import com.ndurance.user_servince.entity.PasswordResetTokenEntity;
import com.ndurance.user_servince.entity.RoleEntity;
import com.ndurance.user_servince.entity.UserEntity;
import com.ndurance.user_servince.service.UserService;
import com.ndurance.user_servince.shared.AmazonSES;
import com.ndurance.user_servince.shared.dto.AddressDTO;
import com.ndurance.user_servince.shared.dto.UserDto;
import com.ndurance.user_servince.shared.model.request.UserPasswordReset;
import com.ndurance.user_servince.shared.model.response.ErrorMessages;
import org.modelmapper.ModelMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@Service
public class UserServiceImpl implements UserService {

	@Autowired
    UserRepository userRepository;

	@Autowired
    Utils utils;

	@Autowired
	BCryptPasswordEncoder bCryptPasswordEncoder;
	
	@Autowired
    PasswordResetTokenRepository passwordResetTokenRepository;
	
	@Autowired
	AmazonSES amazonSES;
	
	@Autowired
    RoleRepository roleRepository;

	@Autowired
	AddressRepository addressRepository;

	@Autowired
    AuthorityRepository authorityRepo;
	private final ModelMapper modelMapper = new ModelMapper();

	private static final String UPLOAD_DIR = System.getProperty("user.home") + "/uploads/";

	@Override
	public UserDto createUser(UserDto user) {
		if (userRepository.findByEmail(user.getEmail()) != null)
			throw new UserServiceException("Record already exists");

		for(int i=0;i<user.getAddresses().size();i++)
		{
			AddressDTO address = user.getAddresses().get(i);
			address.setUserDetails(user);
			address.setAddressId(utils.generateAddressId(30));
			user.getAddresses().set(i, address);
		}

		//BeanUtils.copyProperties(user, userEntity);
		UserEntity userEntity = modelMapper.map(user, UserEntity.class);

		String publicUserId = utils.generateUserId(30);
		userEntity.setUserId(publicUserId);
		userEntity.setEncryptedPassword(bCryptPasswordEncoder.encode(user.getPassword()));
		userEntity.setEmailVerificationToken(utils.generateEmailVerificationToken(publicUserId));

		Collection<RoleEntity> roleEntities = new HashSet<>();
		for(String role: user.getRoles()) {
			RoleEntity roleEntity = roleRepository.findByName(role);
			if(roleEntity !=null) {
				roleEntities.add(roleEntity);
			}
		}

		userEntity.setRoles(roleEntities);
		UserEntity storedUserDetails = userRepository.save(userEntity);

		// Send an email message to user to verify their email address
		//amazonSES.verifyEmail(returnValue);

		return modelMapper.map(storedUserDetails, UserDto.class);
	}

	@Override
	public UserDto updateUserProfile(MultipartFile file, String userid) throws IOException {
		UserEntity user = userRepository.findByUserId(userid);
		if (user == null)
			throw new UserServiceException(ErrorMessages.NO_RECORD_FOUND.getErrorMessage());

		Path uploadPath = Paths.get(UPLOAD_DIR).toAbsolutePath().normalize();
		Files.createDirectories(uploadPath);

		String fileName = utils.generateUserId(10) + file.getOriginalFilename();
		user.setProfilePic(fileName);

		Path filePath = uploadPath.resolve(Objects.requireNonNull(fileName));
		file.transferTo(filePath.toFile());

		UserEntity save = userRepository.save(user);
		return modelMapper.map(save, UserDto.class);
	}

	@Override
	public UserDto getUser(String email) {
		UserEntity userEntity = userRepository.findByEmail(email);

		if (userEntity == null)
			throw new UsernameNotFoundException(email);

		return modelMapper.map(userEntity, UserDto.class);
	}

	@Override
	public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
		UserEntity userEntity = userRepository.findByEmail(email);

		if (userEntity == null)
			throw new UsernameNotFoundException(email);
		
		return new UserPrincipal(userEntity);
	}

	@Override
	public UserDto getUserByUserId(String userId) {
		UserEntity userEntity = userRepository.findByUserId(userId);

		if (userEntity == null)
			throw new UsernameNotFoundException("User with ID: " + userId + " not found");

		UserDto userDto = modelMapper.map(userEntity, UserDto.class);
		List<String> roles = new ArrayList<>();
		List<String> auths = new ArrayList<>();

		userEntity.getRoles().forEach(roleEntity -> {
			roles.add(roleEntity.getName());
			roleEntity.getAuthorities().forEach(authorityEntity -> {
				auths.add(authorityEntity.getName());
			});
		});

		userDto.setAuthorities(auths);
		userDto.setRoles(roles);
		return userDto;
	}

//	@Override
//	public UserDto updateUser(String userId, UserDto user) {
//		UserDto returnValue = new UserDto();
//
//		UserEntity userEntity = userRepository.findByUserId(userId);
//
//
//		if (userEntity == null)
//			throw new UserServiceException(ErrorMessages.NO_RECORD_FOUND.getErrorMessage());
//
//		userEntity.setFirstName(user.getFirstName());
//		userEntity.setLastName(user.getLastName());
//
//		UserEntity updatedUserDetails = userRepository.save(userEntity);
//		returnValue = new ModelMapper().map(updatedUserDetails, UserDto.class);
//
//		return returnValue;
//	}

	@Override
	public UserDto updateUser(String userId, UserDto userDto) {
		UserEntity userEntity = userRepository.findByUserId(userId);

		if (userEntity == null) {
			throw new UserServiceException(ErrorMessages.NO_RECORD_FOUND.getErrorMessage());
		}
		if (userDto.getFirstName() != null && !userDto.getFirstName().trim().isEmpty()) {
			userEntity.setFirstName(userDto.getFirstName());
		}

		if (userDto.getLastName() != null && !userDto.getLastName().trim().isEmpty()) {
			userEntity.setLastName(userDto.getLastName());
		}

		if (userDto.getEmail() != null && !userDto.getEmail().trim().isEmpty()) {
			userEntity.setEmail(userDto.getEmail());
		}

		if(!userEntity.getAddresses().isEmpty()){
			AddressEntity addressEntity = userEntity.getAddresses().get(userEntity.getDefaultAddress());
			AddressDTO addressDTO = userDto.getAddresses().get(0);
			addressEntity.setCity(addressDTO.getCity());
			addressEntity.setCountry(addressDTO.getCountry());
			addressEntity.setStreetName(addressDTO.getStreetName());
			addressEntity.setStreetName(addressDTO.getStreetName());
			addressRepository.save(addressEntity);
		}


		// Handle addresses update
		if (userDto.getAddresses() != null && !userDto.getAddresses().isEmpty()) {
			List<AddressEntity> updatedAddresses = new ArrayList<>();
			for (AddressDTO addressDTO : userDto.getAddresses()) {
				AddressEntity addressEntity = addressRepository.findByAddressId(addressDTO.getAddressId());

				if (addressEntity == null) {
					// Create a new address if it doesn't exist
					addressEntity = new AddressEntity();
					addressEntity.setAddressId(utils.generateAddressId(30));
					addressEntity.setUserDetails(userEntity); // Link the address to the user
				}

				// Update address details
				addressEntity.setCity(addressDTO.getCity());
				addressEntity.setCountry(addressDTO.getCountry());
				addressEntity.setStreetName(addressDTO.getStreetName());
				addressEntity.setPostalCode(addressDTO.getPostalCode());

				updatedAddresses.add(addressEntity);
			}

			// Set the updated addresses to the user
			userEntity.setAddresses(updatedAddresses);
		}
		// Save the updated user entity
		UserEntity updatedUserEntity = userRepository.save(userEntity);

		// Return the updated user as a DTO
		return new ModelMapper().map(updatedUserEntity, UserDto.class);
	}

	@Transactional
	@Override
	public void deleteUser(String userId) {
		UserEntity userEntity = userRepository.findByUserId(userId);

		if (userEntity == null)
			throw new UserServiceException(ErrorMessages.NO_RECORD_FOUND.getErrorMessage());

		userRepository.delete(userEntity);

	}

	@Override
	public List<UserDto> getUsers(int page, int limit) {
		List<UserDto> returnValue = new ArrayList<>();
		
		if(page>0) page = page-1;
		
		Pageable pageableRequest = PageRequest.of(page, limit);
		
		Page<UserEntity> usersPage = userRepository.findAll(pageableRequest);
		List<UserEntity> users = usersPage.getContent();
		
        for (UserEntity userEntity : users) {
            UserDto userDto = new UserDto();
            BeanUtils.copyProperties(userEntity, userDto);
            returnValue.add(userDto);
        }
		
		return returnValue;
	}

	@Override
	public boolean verifyEmailToken(String token) {
	    boolean returnValue = false;

        // Find user by token
        UserEntity userEntity = userRepository.findUserByEmailVerificationToken(token);

        if (userEntity != null) {
            boolean hastokenExpired = Utils.hasTokenExpired(token);
            if (!hastokenExpired) {
                userEntity.setEmailVerificationToken(null);
                userEntity.setEmailVerificationStatus(Boolean.TRUE);
                userRepository.save(userEntity);
                returnValue = true;
            }
        }

        return returnValue;
	}

	@Override
	public boolean requestPasswordReset(String email) {
		
        boolean returnValue = false;
        
        UserEntity userEntity = userRepository.findByEmail(email);

        if (userEntity == null) {
            return returnValue;
        }
        
        String token = new Utils().generatePasswordResetToken(userEntity.getUserId());
        
        PasswordResetTokenEntity passwordResetTokenEntity = new PasswordResetTokenEntity();
        passwordResetTokenEntity.setToken(token);
        passwordResetTokenEntity.setUserDetails(userEntity);
        passwordResetTokenRepository.save(passwordResetTokenEntity);
        
        returnValue = new AmazonSES().sendPasswordResetRequest(
                userEntity.getFirstName(), 
                userEntity.getEmail(),
                token);
        
		return returnValue;
	}

	@Override
	public boolean resetPassword(String token, String password) {
        boolean returnValue = false;
        
        if( Utils.hasTokenExpired(token) )
        {
            return returnValue;
        }
 
        PasswordResetTokenEntity passwordResetTokenEntity = passwordResetTokenRepository.findByToken(token);

        if (passwordResetTokenEntity == null) {
            return returnValue;
        }

        // Prepare new password
        String encodedPassword = bCryptPasswordEncoder.encode(password);
        
        // Update User password in database
        UserEntity userEntity = passwordResetTokenEntity.getUserDetails();
        userEntity.setEncryptedPassword(encodedPassword);
        UserEntity savedUserEntity = userRepository.save(userEntity);
 
        // Verify if password was saved successfully
        if (savedUserEntity != null && savedUserEntity.getEncryptedPassword().equalsIgnoreCase(encodedPassword)) {
            returnValue = true;
        }
   
        // Remove Password Reset token from database
        passwordResetTokenRepository.delete(passwordResetTokenEntity);
        
        return returnValue;
	}

	@Override
	public Resource loadImageAsResource(String imageName) throws MalformedURLException {
		Path filePath = Paths.get(UPLOAD_DIR).resolve(imageName).normalize();
		Resource resource = new UrlResource(filePath.toUri());
		if (resource.exists()) {
			return resource;
		} else {
			throw new UserServiceException(ErrorMessages.NO_RECORD_FOUND.getErrorMessage());
		}
	}

	@Override
	public UserEntity getUserByE(String email) {
		return userRepository.findByEmail(email);
	}

	@Override
	public Resource getImage(String userId) throws MalformedURLException {
		String profilePic = userRepository.findByUserId(userId).getProfilePic();
		return this.loadImageAsResource(profilePic);
	}

	@Override
	public void resetPassWord(UserPasswordReset userPasswordReset) {
		UserEntity user = userRepository.findByUserId(userPasswordReset.getUserId());
		if(bCryptPasswordEncoder.matches(userPasswordReset.getCurrentPassword(), user.getEncryptedPassword())){
			if(userPasswordReset.getNewPassword().equals(userPasswordReset.getConfirmPassword())){
				user.setEncryptedPassword(bCryptPasswordEncoder.encode(userPasswordReset.getNewPassword()));
				userRepository.save(user);
			}
		}
	}

	@Override
	public boolean userAddress(String userId) {
		UserEntity userEntity = userRepository.findByUserId(userId);
		if(userEntity.getAddresses() == null){
			return false;
		} else if (userEntity.getAddresses().isEmpty()) {
			return false;
		}else{
			return true;
		}
	}

}
