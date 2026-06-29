package com.fleetwise.api.telematics.controller;

import com.fleetwise.api.telematics.azure.GeometrisServiceBusSender;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/testing")
public class TestingController {

    private final GeometrisServiceBusSender sender;

    @PostMapping("/servicebus")
    public void send() {

        sender.send("""
            F001,87X061350079,HARDBRAKE,1782161645,29.131252,-82.194018,96,0,1,0,6,227,128426.5,6,,3314,941,953,81,6,128426.4T,KNDJN2A21E7088217,55,1:1:P0130,16,0,0,0
            """);
    }
}