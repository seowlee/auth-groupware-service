package pharos.groupware.service.auth.service;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

@Service
public class LocalUserDetailService implements UserDetailsService {
    @Override
    public UserDetails loadUserByUsername(String username) {
//        UserEntity u = userRepo.findByUsername(username)
//                .orElseThrow(() -> new UsernameNotFoundException(username));
//        return new org.springframework.security.core.userdetails.User(
//                u.getUsername(),
//                u.getPassword(),
//                List.of(new SimpleGrantedAuthority("ROLE_USER"))
//        );
        return null;
    }
}
