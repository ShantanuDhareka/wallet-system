package com.shantanu.wallet.config;

import com.shantanu.wallet.entity.Role;
import com.shantanu.wallet.entity.User;
import com.shantanu.wallet.entity.Wallet;
import com.shantanu.wallet.repository.UserRepository;
import com.shantanu.wallet.repository.WalletRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner createAdmin(
            UserRepository userRepository,
            WalletRepository walletRepository,
            PasswordEncoder passwordEncoder) {

        return args -> {

            if (userRepository.findByEmail("admin@wallet.com").isEmpty()) {

                User admin = new User();
                admin.setEmail("admin@wallet.com");
                admin.setPassword(passwordEncoder.encode("Admin@123"));
                admin.setRole(Role.ADMIN);

                userRepository.save(admin);

                Wallet wallet = new Wallet();
                wallet.setUser(admin);
                wallet.setBalance(BigDecimal.ZERO);

                walletRepository.save(wallet);
            }
        };
    }
}