package com.nowcoder.community;

import java.io.IOException;

public class WKTest {
    public static void main(String[] args) {
        String cmd="d:/work/wkhtmltopdf/bin/wkhtmltoimage --quality 75 http://www.sina.com d:/work/data/wk-image/3.png";
        try {
            Runtime.getRuntime().exec(cmd);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
