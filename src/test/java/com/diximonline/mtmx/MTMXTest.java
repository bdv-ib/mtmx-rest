package com.diximonline.mtmx;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;

import lombok.extern.slf4j.Slf4j;
import se.highex.ebe.adaptor.connector.pg.PgParamsMtMsg;
import se.highex.mtmx.MtMx;
import se.highex.mtmx.MtMxException;

@Slf4j
public class MTMXTest {

	@Test
	@DisplayName("Test MT198_195 to MX convertion")
	void mtToMX() {
		final MtMx converter = new MtMx();
		final String block4 =
    		":20:REFUSAL/001\r\n" +
    		":12:195\r\n" +
    		":77E:\r\n" +
    		":21:415389/001\r\n" +
    		":75:RJCT\r\n" +
    		":77A:EA01\r\n" +
    		":11S:104\r\n" +
    		"150710\r\n" +
    		":79:ABIBBHBM\r\n" +
    		"150703\r\n" +
    		"MT104ABIB109";
    
		PgParamsMtMsg mtMsg = new PgParamsMtMsg();

		mtMsg.setMsgUserReference("USERREFERENCE");
		mtMsg.setMsgSender("HABBBHBMXXXX");
		mtMsg.setMsgReceiver("BMAGBHBMXEFT");
		mtMsg.setMsgType("198");
		mtMsg.setMsgPriority("N");
		mtMsg.setMsgUserPriority("0100");
		mtMsg.setFormat("MT");
		mtMsg.setBlock4(block4);
		
		try {
			String mtXML = toXML(mtMsg, true);
			log.debug("MT: {}", mtXML);
		} catch (Exception e) {
			e.printStackTrace();
		}

		PgParamsMtMsg resultsMX = null;

		try {
			resultsMX = converter.ConvertMTToMX(mtMsg);
		} catch (MtMxException e) {
			System.out.print("Unknown error: " + e.getLocalizedMessage());
		}
		
		try {
			log.debug("MX: {}", toXML(resultsMX, true));
		} catch (Exception e) {
			e.printStackTrace();
		}
    
		assertEquals(resultsMX.getMsgType(), "pacs.002.001.05");
      
		PgParamsMtMsg resultsMT = null;
      
		try {
			resultsMT = converter.ConvertMXToMT(resultsMX);
		} catch (MtMxException e) {
			System.out.print("Unknown error: " + e.getLocalizedMessage());
		}
      
		assertEquals(resultsMT.getMsgType(), "198");
	}
  
	public <T> String toXML(T object, boolean ifThrow) throws Exception {
		if (object == null)
			return null;

		XmlMapper  mapper = new XmlMapper ();

		try {
			return mapper.writeValueAsString(object);
		} catch (JsonProcessingException  e) {
			if (ifThrow)
				throw e;

			return null;
		}
	}
}
