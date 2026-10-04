package com.diximonline.mtmx.rest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import se.highex.ebe.adaptor.connector.pg.PgParamsMtMsg;
import se.highex.mtmx.MtMx;


@Slf4j
@RestController
@RequestMapping("${mtmx.controller.baseUri:#{null}}")
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class MtMxController {

	@PostMapping("/convert")
	public ResponseEntity<String> convertMTMX(@RequestHeader Map<String, String> headers, @RequestBody String body) {

		log.info("[START] CBS REST | MTMX converter: {}", body);

		try {
			List<PgParamsMtMsg> msgList = fromXML(body, true, new TypeReference<List<PgParamsMtMsg>>() {});
			List<PgParamsMtMsg> convMsgList = new ArrayList<PgParamsMtMsg>();
			final MtMx converter = new MtMx();
			
			for (PgParamsMtMsg msg : msgList) {
			
				PgParamsMtMsg convertedMsg = null;
				
				if (msg != null) {
					if (msg.getFormat().compareTo(PgParamsMtMsg.TYPE_MT) == 0) {
						convertedMsg = converter.ConvertMTToMX(msg);
						
						String block4MX = convertedMsg.getBlock4();
						String msgType = convertedMsg.getMsgType();
						
						block4MX = addNamespaceString(block4MX, "DataPDU", "urn:swift:saa:xsd:saa.2.0");
						block4MX = addNamespaceString(block4MX, "AppHdr", "urn:iso:std:iso:20022:tech:xsd:head.001.001.01");
						block4MX = addNamespaceString(block4MX, "Document", "urn:iso:std:iso:20022:tech:xsd:" + msgType);
						
						convertedMsg.setBlock4(block4MX);
						
					} else if (msg.getFormat().compareTo(PgParamsMtMsg.TYPE_MX) == 0) {
						convertedMsg = converter.ConvertMXToMT(msg);
					} else {
						throw new Exception("Wrong message type");
					}
				} else {
					throw new Exception("Wrong message format");
				}
				
				convMsgList.add(convertedMsg);
			}
			
			return ResponseEntity.ok().body(toXML(convMsgList, true));
		} catch (Exception e) {
			log.error("CBS REST | Error happened during MTMX convertion: ", e);
			return ResponseEntity.badRequest().body(e.getLocalizedMessage());
		}
	}
	
    public String addNamespaceString(String xml, String rootTag, String ns) {
        return xml.replaceFirst("<" + rootTag + "(\\s*)>", "<" + rootTag + " xmlns=\"" + ns + "\"$1>");
    }
  
	public <T> T fromXML(String xml, boolean ignoreUnknownProps, TypeReference<T> typeReference) throws Exception {
		
		if (StringUtils.isEmpty(xml))
			return null;

		XmlMapper  mapper = new XmlMapper ();

		if (ignoreUnknownProps) {
			mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
			mapper.enable(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL);
		}

		try {
			return mapper.readValue(xml, typeReference);
		} catch (JsonProcessingException e) {
			log.error("fromXML failed for class:" + typeReference, e);
			throw e;
		}
	}

	public <T> String toXML(T object, boolean ifThrow) throws Exception {
		if (object == null)
			return null;

		XmlMapper  mapper = new XmlMapper ();

		try {
			return mapper.writeValueAsString(object);
		} catch (JsonProcessingException  e) {
			log.error("toXML failed for class:" + object.getClass(), e);
			if (ifThrow)
				throw e;

			return null;
		}
	}
}
