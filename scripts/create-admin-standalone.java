import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.lebvest.model.entities.investor.User;
import com.lebvest.model.enums.Role;
import com.lebvest.repository.UserRepository;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@SpringBootApplication
@ComponentScan(basePackages = "com.lebvest")
@EntityScan(basePackages = "com.lebvest.model.entities")
@EnableJpaRepositories(basePackages = "com.lebvest.repository")
public class CreateAdminStandalone {

    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(CreateAdminStandalone.class, args);
        
        UserRepository userRepository = context.getBean(UserRepository.class);
        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        
        String email = args.length > 0 ? args[0] : "admin@lebvest.com";
        String password = args.length > 1 ? args[1] : "admin123";
        String name = args.length > 2 ? args[2] : "Admin User";
        
        System.out.println("Creating admin user...");
        System.out.println("Email: " + email);
        System.out.println("Name: " + name);
        System.out.println();
        
        Optional<User> existingUser = userRepository.findByEmail(email);
        if (existingUser.isPresent()) {
            User user = existingUser.get();
            Set<Role> roles = user.getRoles();
            if (roles == null) {
                roles = new HashSet<>();
            } else {
                roles = new HashSet<>(roles);
            }
            
            if (roles.contains(Role.ADMIN)) {
                System.out.println("User " + email + " already exists with ADMIN role");
                context.close();
                return;
            }
            
            roles.add(Role.ADMIN);
            user.setRoles(roles);
            user.setPassword(passwordEncoder.encode(password));
            user.setEnabled(true);
            user.setLocked(false);
            userRepository.save(user);
            System.out.println("Updated existing user " + email + " with ADMIN role");
            context.close();
            return;
        }
        
        Set<Role> roles = new HashSet<>();
        roles.add(Role.ADMIN);
        
        User adminUser = User.builder()
                .email(email)
                .password(passwordEncoder.encode(password))
                .name(name)
                .roles(roles)
                .enabled(true)
                .locked(false)
                .build();
        
        userRepository.save(adminUser);
        System.out.println("Admin user created successfully!");
        System.out.println("Email: " + email);
        System.out.println("Password: " + password);
        System.out.println("Name: " + name);
        
        context.close();
    }
}

