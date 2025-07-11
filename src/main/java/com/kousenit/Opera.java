package com.kousenit;

import java.util.List;

public record Opera(
        String title,
        String premise,
        List<Scene> scenes
) {
    public record Scene(
            int number,
            String title,
            String author,
            String content
    ) {
        public String getFileName() {
            return String.format("scene_%d_%s.txt",
                    number,
                    title.replaceAll("\\s+", "_").toLowerCase());
        }

        public String getImageFileName() {
            return String.format("scene_%d_illustration.png", number);
        }
    }
}