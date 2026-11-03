package com.carebridge.rescheduling.api;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class FaviconController {

    @GetMapping("favicon.ico")
    @ResponseBody
    public ResponseEntity<Void> returnNoFavicon() {
        return ResponseEntity.noContent().build();
    }
}
