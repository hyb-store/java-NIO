package com.hyb.client;

import java.io.IOException;

public class BClient {

    public static void main(String[] args) {
        try {
            new ChatClient().startClient("BClient");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
