package es.hackUDC.slAIds.rest.controllers;

import static es.hackUDC.slAIds.rest.dtos.UserConversor.toAuthenticatedUserDto;
import static es.hackUDC.slAIds.rest.dtos.UserConversor.toUser;
import java.net.URI;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import es.hackUDC.slAIds.model.exceptions.InstanceNotFoundException;
import es.hackUDC.slAIds.model.entities.ModelUser;
import es.hackUDC.slAIds.model.exceptions.DuplicateInstanceException;
import es.hackUDC.slAIds.model.exceptions.IncorrectLoginException;
import es.hackUDC.slAIds.model.exceptions.IncorrectPasswordException;
import es.hackUDC.slAIds.model.services.UserService.UserService;
import es.hackUDC.slAIds.rest.common.ErrorsDto;
import es.hackUDC.slAIds.rest.common.JwtGenerator;
import es.hackUDC.slAIds.rest.common.JwtInfo;
import es.hackUDC.slAIds.rest.dtos.AuthenticatedUserDto;
import es.hackUDC.slAIds.rest.dtos.LoginParamsDto;
import es.hackUDC.slAIds.rest.dtos.UserDto;

@RestController
@RequestMapping("/users")
public class UserController {

    @Autowired
    private JwtGenerator jwtGenerator;

    @Autowired
    private UserService userService;

    @ExceptionHandler(InstanceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ResponseBody
    public ErrorsDto handleIncorrectLoginException(InstanceNotFoundException exception) {

        return new ErrorsDto("The user: " + exception.getKey() + " does not exist");

    }

    @ExceptionHandler(IncorrectLoginException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ResponseBody
    public ErrorsDto handleIncorrectLoginException(IncorrectLoginException exception) {

        return new ErrorsDto("Incorrect login");

    }

    @ExceptionHandler(IncorrectPasswordException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ResponseBody
    public ErrorsDto handleIncorrectPasswordException(IncorrectPasswordException exception) {

        return new ErrorsDto("Incorrect password");

    }

    @ExceptionHandler(DuplicateInstanceException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    @ResponseBody
    public ErrorsDto handleDuplicateInstanceException(DuplicateInstanceException exception) {
        return new ErrorsDto("The user: " + exception.getKey() + " already exists");
    }

    @PostMapping("/signup")
    public ResponseEntity<AuthenticatedUserDto> signUp(
            @Validated({ UserDto.AllValidations.class }) @RequestBody UserDto userDto)
            throws DuplicateInstanceException {

        ModelUser user = toUser(userDto);

        userService.signUp(user);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest().path("/{id}")
                .buildAndExpand(user.getId()).toUri();

        return ResponseEntity.created(location).body(toAuthenticatedUserDto(generateServiceToken(user), user));

    }

    @PostMapping("/login")
    public AuthenticatedUserDto login(@Validated @RequestBody LoginParamsDto params)
            throws IncorrectLoginException {

        ModelUser user = userService.login(params.getUserName(), params.getPassword());

        return toAuthenticatedUserDto(generateServiceToken(user), user);

    }

    @PostMapping("/loginFromServiceToken")
    public AuthenticatedUserDto loginFromServiceToken(@RequestAttribute Long userId,
            @RequestAttribute String serviceToken) throws InstanceNotFoundException {

        ModelUser user = userService.loginFromId(userId);

        return toAuthenticatedUserDto(serviceToken, user);

    }

    private String generateServiceToken(ModelUser user) {

        JwtInfo jwtInfo = new JwtInfo(user.getId(), user.getUserName(), user.getRole().toString());

        return jwtGenerator.generate(jwtInfo);

    }

}
