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

		String[] lines = httpHeader.split("\r\n");
		System.out.println("***** Lines ****\n" + lines + "\n**********************");

        for (String line : lines) {
            if (line.contains(":")) {
                String[] fields = line.split(":");
                System.out.println("****** Fields ***** \n" + fields + "\n*********************");
                attributes.put(fields[0],fields[1]);
            }
        }


	}

    boolean checkContentLength() {
        if (attributes.containsKey("Content-Length")) {
            return true;
        }
        else
            return false;
    }

    int getContentLength() {
        if (attributes.containsKey("Content-Length")) {
            String length = attributes.get("Content-Length");
            length = length.substring(1);
            int intLength = Integer.parseInt(length);
            System.out.println("Length ermittelt!");

            return intLength;
        }
        else return 0;
    }

    boolean checkData() {
        String[] data = httpHeader.split("\r\n\r\n");
        return data.length > 1;
    }

    String getData() {
        String[] data = httpHeader.split("\r\n\r\n");
        if (data.length > 1) {
            return data[1];
        }
        else return "keine Daten";
    }

    int getDataLength() {
        String data = getData();
        return data.length();
    }




}
