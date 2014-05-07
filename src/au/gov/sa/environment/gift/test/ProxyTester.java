package au.gov.sa.environment.gift.test;

import au.gov.sa.environment.gift.http.SPMessage;
import au.gov.sa.environment.gift.http.SharePointProxy;

public class ProxyTester {

	public static void main(String[] args) {
		// TODO Auto-generated method stub

		SharePointProxy proxy = new SharePointProxy(null);
		SPMessage message = new SPMessage();
		message = proxy.performFunction(message);
		System.out.println(message.toString());
	}

}
