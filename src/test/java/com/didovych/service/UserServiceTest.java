package com.didovych.service;

import com.didovych.domain.Role;
import com.didovych.domain.User;
import com.didovych.repos.UserRepo;
import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Collections;

@RunWith(SpringRunner.class)
@SpringBootTest
public class UserServiceTest {

    @Autowired
    private UserService userService;

    @MockBean
    private UserRepo userRepo;

    @MockBean
    private MailSender mailSender;

    @MockBean
    private PasswordEncoder passwordEncoder;

    @Test
    public void addUser() {
        User user = new User();

        user.setEmail("some@mail");
        user.setPassword("111");

        boolean isUserCreated = userService.addUser(user);

        Assert.assertTrue(isUserCreated);
        Assert.assertNotNull(user.getActivationCode());
        Assert.assertTrue(CoreMatchers.is(user.getRoles()).matches(Collections.singleton(Role.USER)));
        Assert.assertTrue(CoreMatchers.is(passwordEncoder.encode("111")).matches(user.getPassword()));

        Mockito.verify(userRepo, Mockito.times(1)).save(user);
        Mockito.verify(mailSender, Mockito.times(1))
                .send(
                        ArgumentMatchers.eq(user.getEmail()),
                        ArgumentMatchers.anyString(),
                        ArgumentMatchers.anyString()
                );
    }

    @Test
    public void addUserFailTest() {
        User user = new User();

        user.setUsername("John");

        Mockito.doReturn(new User())
                .when(userRepo)
                .findByUsername("John");

        boolean isUserCreated = userService.addUser(user);

        Assert.assertFalse(isUserCreated);

        Mockito.verify(userRepo, Mockito.times(0)).save(ArgumentMatchers.any(User.class));
        Mockito.verify(mailSender, Mockito.times(0))
                .send(
                        ArgumentMatchers.anyString(),
                        ArgumentMatchers.anyString(),
                        ArgumentMatchers.anyString()
                );
    }

    @Test
    public void activateUser() {
        User user = new User();

        user.setActivationCode("bingo!");

        Mockito.doReturn(user)
                .when(userRepo)
                .findByActivationCode("activate");

        boolean isUserActivated = userService.activateUser("activate");

        Assert.assertTrue(isUserActivated);
        Assert.assertNull(user.getActivationCode());

        Mockito.verify(userRepo, Mockito.times(1)).save(user);
    }

    @Test
    public void activateUserFailTest() {
        boolean isUserActivated = userService.activateUser("activate me");

        Assert.assertFalse(isUserActivated);

        Mockito.verify(userRepo, Mockito.times(0)).save(ArgumentMatchers.any(User.class));
    }

    @Test
    public void loadUserByUsernameTest() {

        Mockito.doReturn(new User())
                .when(userRepo)
                .findByUsername("John");

        User john = (User) userService.loadUserByUsername("John");

        Assert.assertNotNull(john);
    }

    @Test(expected = UsernameNotFoundException.class)
    public void loadUserByUsernameFailTest() {
        userService.loadUserByUsername("Ross");
    }


    @Test
    public void updateEmailTest() {
        String newEmail = "another@email";
        User user = new User();
        user.setEmail("current@mail");

        userService.updateProfile(user, "", newEmail);

        Assert.assertNotNull(user.getActivationCode());
        Assert.assertTrue(CoreMatchers.is(user.getEmail()).matches("another@email"));

        Mockito.verify(userRepo, Mockito.times(1)).save(user);

        Mockito.verify(mailSender, Mockito.times(1)).send(
                ArgumentMatchers.matches(user.getEmail()),
                ArgumentMatchers.anyString(),
                ArgumentMatchers.anyString()
        );
    }

    @Test
    public void updateEmailIfTheSameEnteredTest() {
        String newEmail = "current@mail";
        User user = new User();
        user.setEmail("current@mail");

        userService.updateProfile(user, null, newEmail);

        Assert.assertNull(user.getActivationCode());

        Mockito.verify(userRepo, Mockito.times(1)).save(user);
        Mockito.verify(mailSender, Mockito.times(0)).send(
                ArgumentMatchers.anyString(),
                ArgumentMatchers.anyString(),
                ArgumentMatchers.anyString()
        );
    }

    @Test
    public void changePasswordOnly() {
        String newPassword = "000";
        User user = new User();
        user.setPassword("111");
        userService.updateProfile(user, newPassword, null);

        Assert.assertNull(user.getActivationCode());
        Assert.assertTrue(CoreMatchers.is(user.getPassword())
                .matches(passwordEncoder.encode(newPassword)));


        Mockito.verify(userRepo, Mockito.times(1)).save(user);

        Mockito.verify(mailSender, Mockito.times(0)).send(
                ArgumentMatchers.anyString(),
                ArgumentMatchers.anyString(),
                ArgumentMatchers.anyString()

        );
    }
}