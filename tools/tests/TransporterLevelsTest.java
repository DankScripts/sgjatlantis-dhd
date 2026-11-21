package com.mustangdoc.sgjpatch.transporter;

public final class TransporterLevelsTest {
    public static void main(String[] args) {
        check(TransporterLevels.isLower(-3885, 255, 5885, "Renamed"), "Lower Spire anchor");
        check(TransporterLevels.isLower(-3937, 263, 5973, "Renamed"), "Genetics Lab anchor");
        check(TransporterLevels.isLower(0, 0, 0, "GENETICS LAB"), "named Genetics Lab");
        check(TransporterLevels.isLower(0, 0, 0, "Lower Spire"), "named Lower Spire");
        check(TransporterLevels.isLower(-3897, 256, 5876, "Renamed"), "legacy lower bay");
        check(TransporterLevels.isLower(0, 0, 0, "Underwater Jumper Bay"), "underwater bay");
        check(!TransporterLevels.isLower(-4268, 326, 5886, "West City"), "upper city");
        check(!TransporterLevels.isLower(1, 200, 1, "Custom Station"), "unrelated custom station");
        System.out.println("PASS: transporter lower-city destinations and existing station behavior");
    }

    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
}
