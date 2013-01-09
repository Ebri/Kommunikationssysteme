package praktikum.beispiele.beispiel1;


/**
 * User: joachim
 * Date: 05.12.12
 * Time: 16:35
 */

class HttpHeaderParser {

	String httpHeader;

	HashMap<String,String> attributes;

	public HttpHeaderParser(String httpHeader) {
		this.httpHeader = httpHeader;
        attributes = new HashMap<String, String>();

        // Trenne die Zeilen des HTTP-Headers an <CR><LF>
		String[] lines = httpHeader.split("\r\n");

        // Untersuche die Zeilen auf einen Doppelpunkt und trenne die Schlüssel-Attribut-Paare daran.

        try {
            for (String line : lines) {
                if (line.contains(":")) {
                    String[] fields = line.split(":");
                    attributes.put(fields[0],fields[1].trim());
                }
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            e.printStackTrace();
        }


	}

    /**
     * Prüft ob das "Content-Length" Attribut vorhanden ist.
     * @return true - wenn vorhanden
     */
    boolean checkContentLength() {
        return attributes.containsKey("Content-Length");
    }

    /**
     * Gibt den Wert des Content-Length Attributes zurück, wenn dieses vorhanden ist.
     * @return Die Länge des Datenteils, wenn das Attribut vorhanden ist, sonst 0.
     */
    int getContentLength() {
        if (attributes.containsKey("Content-Length")) {
            String length = attributes.get("Content-Length");
            int intLength = Integer.parseInt(length);
            return intLength;
        }
        else return 0;
    }

    /**
     * Prüft ob der Datenteil vorhanden ist bzw. anfängt.
     * @return true - wenn vorhanden, sonst false.
     */
    boolean checkData() {
        String[] data = httpHeader.split("\r\n\r\n");
        return data.length > 1;
    }

    /**
     * Gibt den Datenteil als String zurück.
     * @return die HTTP-Daten.
     */
    String getData() {
        String[] data = httpHeader.split("\r\n\r\n");
        if (data.length > 1) {
            return data[1];
        }
        else return "keine Daten";
    }

    /**
     * Gibt die Länge der bisher Abgerufenen Daten zurück.
     * @return Länge der Daten
     */
    int getDataLength() {
        String data = getData();
        return data.length();
    }




}
