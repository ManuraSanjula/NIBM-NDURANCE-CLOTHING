package com.ndurance.user_servince.contoller;

import com.ndurance.user_servince.exceptions.UserServiceException;
import com.ndurance.user_servince.exceptions.UserUnAuthorizedServiceException;
import com.ndurance.user_servince.shared.model.request.UserDetailsRequestModel;
import com.ndurance.user_servince.shared.model.request.UserPasswordReset;
import com.ndurance.user_servince.shared.model.response.*;
import com.ndurance.user_servince.service.AddressService;
import com.ndurance.user_servince.service.UserService;
import com.ndurance.user_servince.shared.Roles;
import com.ndurance.user_servince.shared.dto.AddressDTO;
import com.ndurance.user_servince.shared.dto.UserDto;
import org.modelmapper.ModelMapper;
import org.modelmapper.TypeToken;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.util.*;

@RestController
@RequestMapping("/users")
public class UserController {

	@Autowired
	UserService userService;

	@Autowired
	AddressService addressService;

	@Autowired
	AddressService addressesService;

	@GetMapping(path = "/{userid}")
	public UserRest getUser(@PathVariable String userid) {
		UserDto userDto = userService.getUserByUserId(userid);
		ModelMapper modelMapper = new ModelMapper();
		return modelMapper.map(userDto, UserRest.class);
	}
	
	@PutMapping("/upload-pic/{userid}")
	public UserRest updateUserProfile(@PathVariable String userid, @RequestParam("image") MultipartFile file) throws Exception {
		ModelMapper modelMapper = new ModelMapper();
		UserDto createdUser = userService.updateUserProfile(file, userid);
		return modelMapper.map(createdUser, UserRest.class);
	}

	@PostMapping()
	public UserRest createUser(@RequestBody UserDetailsRequestModel userDetails) throws Exception {
		UserRest returnValue = new UserRest();

		ModelMapper modelMapper = new ModelMapper();
		UserDto userDto = modelMapper.map(userDetails, UserDto.class);
		userDto.setRoles(new HashSet<>(Arrays.asList(Roles.ROLE_USER.name())));

		UserDto createdUser = userService.createUser(userDto);
		returnValue = modelMapper.map(createdUser, UserRest.class);

		return returnValue;
	}


	@PutMapping(path = "/{userid}")
	public UserRest updateUser(@PathVariable String userid, @RequestBody UserDetailsRequestModel userDetails) {
		UserRest returnValue = new UserRest();

		UserDto userDto = new UserDto();
		userDto = new ModelMapper().map(userDetails, UserDto.class);

		UserDto updateUser = userService.updateUser(userid, userDto);
		returnValue = new ModelMapper().map(updateUser, UserRest.class);

		return returnValue;
	}

	@DeleteMapping(path = "/{userid}")
	public OperationStatusModel deleteUser(@PathVariable String userid) {
		OperationStatusModel returnValue = new OperationStatusModel();
		returnValue.setOperationName(RequestOperationName.DELETE.name());

		userService.deleteUser(userid);

		returnValue.setOperationResult(RequestOperationStatus.SUCCESS.name());
		return returnValue;
	}

	@GetMapping()
	public List<UserRest> getUsers(@RequestParam(value = "page", defaultValue = "0") int page,
			@RequestParam(value = "limit", defaultValue = "2") int limit) {
		List<UserRest> returnValue = new ArrayList<>();

		List<UserDto> users = userService.getUsers(page, limit);
 
		for (UserDto userDto : users) {
			UserRest userModel = new UserRest();
			BeanUtils.copyProperties(userDto, userModel);
			returnValue.add(userModel);
		}

		return returnValue;
	}

	@GetMapping(path = "/{addressId}/addresses", produces = { MediaType.APPLICATION_XML_VALUE,
			MediaType.APPLICATION_JSON_VALUE})
	public CollectionModel<AddressesRest> getUserAddresses(@PathVariable String addressId) {
		List<AddressesRest> returnValue = new ArrayList<>();

		List<AddressDTO> addressesDTO = addressesService.getAddresses(addressId);

		if (addressesDTO != null && !addressesDTO.isEmpty()) {
			Type listType = new TypeToken<List<AddressesRest>>() {
			}.getType();
			returnValue = new ModelMapper().map(addressesDTO, listType);
			
			for (AddressesRest addressRest : returnValue) {
				Link selfLink = WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(UserController.class)
						.getUserAddress(addressId, addressRest.getAddressId()))
						.withSelfRel();
				addressRest.add(selfLink);
			}
			
		}
		
		Link userLink = WebMvcLinkBuilder.linkTo(UserController.class).slash(addressId).withRel("user");
		Link selfLink = WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(UserController.class)
				.getUserAddresses(addressId))
				.withSelfRel();
		return CollectionModel.of(returnValue, userLink, selfLink);
	}

	@GetMapping(path = "/{userId}/addresses/{addressId}", produces = { MediaType.APPLICATION_JSON_VALUE,
			MediaType.APPLICATION_XML_VALUE})
	public EntityModel<AddressesRest> getUserAddress(@PathVariable String userId, @PathVariable String addressId) {

		AddressDTO addressesDto = addressService.getAddress(addressId);

		ModelMapper modelMapper = new ModelMapper();
		AddressesRest returnValue = modelMapper.map(addressesDto, AddressesRest.class);
		
		Link userLink = WebMvcLinkBuilder.linkTo(UserController.class).slash(userId).withRel("user");
		Link userAddressesLink = WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(UserController.class).getUserAddresses(userId))
				.withRel("addresses");
		Link selfLink = WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(UserController.class)
				.getUserAddress(userId, addressId))
				.withSelfRel();

 	
		return EntityModel.of(returnValue, Arrays.asList(userLink,userAddressesLink, selfLink));
	}

	@PostMapping("/reset-password/{userId}")
	public void resetPassWord(@RequestBody UserPasswordReset userPasswordReset, @PathVariable String userId){

		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		String username = authentication.getName();

		if(!Objects.equals(username, userId))
			throw new UserUnAuthorizedServiceException(ErrorMessages.AUTHENTICATION_FAILED.getErrorMessage());

		if(!userPasswordReset.getNewPassword().equals(userPasswordReset.getNewPassword()))
			throw new UserServiceException(ErrorMessages.MISSING_REQUIRED_FIELD.getErrorMessage());

		userService.resetPassWord(userPasswordReset);
	}

	@GetMapping("/image/{userid}")
	public ResponseEntity<Resource> getImage(@PathVariable String userid) {
		try {
			Resource resource = userService.getImage(userid);

			String contentType = "image/jpeg"; // Default to JPEG
			try {
				contentType = Files.probeContentType(resource.getFile().toPath());
			} catch (IOException ex) {
				ex.printStackTrace();
			}

			return ResponseEntity.ok()
					.contentType(MediaType.parseMediaType(contentType))
					.body(resource);
		} catch (Exception e) {
			return ResponseEntity.notFound().build();
		}
	}

}