package com.aiincidentcommander.api_gateway.security;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor

public class AppUser {

    private String userName ;
    private String password ;
    private List<String> roles ;
}
