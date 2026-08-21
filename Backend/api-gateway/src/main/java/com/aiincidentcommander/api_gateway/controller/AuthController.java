package com.aiincidentcommander.api_gateway.controller;


import com.aiincidentcommander.api_gateway.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager ;
    private  final JwtService jwtService ;

    @PostMapping("/login")
    public ResponseEntity<Object> login (@RequestBody LoginRequest request){
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.userName() , request.password()
                    )
            );

        } catch (BadCredentialsException ex) {
            return ResponseEntity.status(401).body("Invalid username or password");
        }
        String token = jwtService.generateToken(request.userName());
        return ResponseEntity.ok(new LoginResponse(token));
    }



        /// /// helper

    public  record LoginRequest(String userName  , String password ){}
    public record LoginResponse (String token ){}


}
