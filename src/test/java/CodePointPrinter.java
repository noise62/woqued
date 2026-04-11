public class CodePointPrinter {
    public static void main(String[] args) {
        String text = "ᴀʙᴄᴅᴇғɢʜɪᴊᴋʟᴍɴᴏᴘǫʀꜱᴛᴜᴠᴡxʏᴊ⚡⏺●┃♯╠ ABCDEFGJHIJKLMNOPQRSTUVWXY";

        System.out.println("Символ | CodePoint | Unicode");
        System.out.println("-------|-----------|---------");

        text.codePoints().forEach(codePoint -> {
            char[] chars = Character.toChars(codePoint);
            String character = new String(chars);
            String unicode = String.format("U+%04X", codePoint);
            System.out.printf("  %s    |   %5d   | %s%n", character, codePoint, unicode);
        });
    }
}