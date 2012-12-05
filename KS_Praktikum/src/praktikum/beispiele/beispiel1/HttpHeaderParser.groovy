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

		String[] lines = httpHeader.split("\r");
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
            String
            return
        }
    }




}
