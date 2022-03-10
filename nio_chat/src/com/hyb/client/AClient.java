package com.hyb.client;

import java.io.IOException;

public class AClient {

    public static void main(String[] args) {
        try {
            new ChatClient().startClient("AClient");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
