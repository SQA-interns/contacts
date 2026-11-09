package si.contactgroups.service;

/** A created Export file: its name, its bytes and the number of exported Contacts. */
public record ExportFile(String fileName, byte[] bytes, int count) {}
