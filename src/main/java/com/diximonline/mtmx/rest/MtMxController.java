package com.diximonline.mtmx.rest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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
import com.diximonline.core.utils.LogUtils.PayloadTrace;
import com.diximonline.mtmx.conv.efts.OperationTypes;


@Slf4j
@RestController
@RequestMapping("${mtmx.controller.baseUri:#{null}}")
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class MtMxController {

	String header_X_Request_ID = "x-request-id";
	String header_X_Fapi_Interaction_ID = "x-fapi-interaction-id";
	
	private MtMx converter = new MtMx();
	
	@PostMapping("/convert")
	public ResponseEntity<String> convertMTMX(@RequestHeader Map<String, String> headers, @RequestBody String body) {

		String requestId = "";
		
		if (headers != null && headers.get(header_X_Request_ID) != null) {
			requestId = headers.get(header_X_Request_ID).toString();
		} else {
			requestId = UUID.randomUUID().toString();
		}
		
		String correlationId = "";
		
		if (headers != null && headers.get(header_X_Fapi_Interaction_ID) != null) {
			correlationId = headers.get(header_X_Fapi_Interaction_ID).toString();
		} else {
			correlationId = "n/a";
		}
		
		log.info("[START] CBS REST | MTMX converter, requestId: {}, correlationId: {}", requestId, correlationId);
		PayloadTrace.write("[BODY] CBS REST | MTMX converter, requestId: " + requestId, body);
		
		long start = System.nanoTime();
		
		try {
			List<PgParamsMtMsg> msgList = fromXML(body, true, new TypeReference<List<PgParamsMtMsg>>() {});
			List<PgParamsMtMsg> convMsgList = new ArrayList<PgParamsMtMsg>();
			
			for (PgParamsMtMsg msg : msgList) {
			
				PgParamsMtMsg convertedMsg = null;
				
				if (msg != null) {
					if (msg.getFormat().compareTo(PgParamsMtMsg.TYPE_MT) == 0) {
						
						long startConv = System.nanoTime();
						
						List<String> kw = OperationTypes.extractKeyWords(msg);
						OperationTypes ot = OperationTypes.detectOperationType(msg.getMsgType(), kw);
						
						if (ot != null && ot.converter != null) {
							log.debug("requestId: {}, found custom internal converter, class {}", requestId, ot.converter.getClass());
							
							try {
								convertedMsg = ot.converter.toModel(msg, null);
							} catch (Exception e) {
								log.error("requestId: {}, internal converter failed, will try to request MTMX service", requestId);
							}
						}

						if (convertedMsg == null) {
							log.debug("requestId: {}, requesting MTMX service", requestId);
							convertedMsg = converter.ConvertMTToMX(msg);
							
							String block4MX = convertedMsg.getBlock4();
							String msgType = convertedMsg.getMsgType();
							
							block4MX = addNamespaceString(block4MX, "DataPDU", "urn:cma:stp:xsd:stp.1.0");
							block4MX = addNamespaceString(block4MX, "AppHdr", "urn:iso:std:iso:20022:tech:xsd:head.001.001.01");
							block4MX = addNamespaceString(block4MX, "Document", "urn:iso:std:iso:20022:tech:xsd:" + msgType);
							
							block4MX = block4MX.replaceAll("(?s)\\s*<Revision>.*?</Revision>", "")
					                   		   .replaceAll("(?s)\\s*<Header>.*?</Header>", "");
							
							convertedMsg.setBlock4(block4MX);
						}
						
						long elapsedConv = System.nanoTime() - startConv;
						log.debug("CBS REST | MTMX converter | ConvertMTToMX, processing time (ms): {}", elapsedConv/1_000_000);
						
					} else if (msg.getFormat().compareTo(PgParamsMtMsg.TYPE_MX) == 0) {
						
						long startConv = System.nanoTime();
						
						convertedMsg = converter.ConvertMXToMT(msg);
						
						long elapsedConv = System.nanoTime() - startConv;
						log.debug("CBS REST | MTMX converter | ConvertMXToMT, processing time (ms): {}", elapsedConv/1_000_000);
					} else {
						throw new Exception("Wrong message type");
					}
				} else {
					throw new Exception("Wrong message format");
				}
				
				convMsgList.add(convertedMsg);
			}
			
			ResponseEntity<String> res = ResponseEntity.ok().body(toXML(convMsgList, true));
			
			long elapsed = System.nanoTime() - start;
			log.info("[END] CBS REST | MTMX converter, requestId: {}, processing time (ms): {}", requestId, elapsed/1_000_000);
			
			return res;
		} catch (Exception e) {
			log.error("[ERROR] CBS REST | Error happened for the requestId {} during MTMX convertion: ", requestId, e);
			return ResponseEntity.badRequest().body(e.getLocalizedMessage());
		}
	}
	
    public String addNamespaceString(String xml, String rootTag, String ns) {
        return xml.replaceFirst("<" + rootTag + "(\\s*)>", "<" + rootTag + " xmlns=\"" + ns + "\"$1>");
    }
  
	public <T> T fromXML(String xml, boolean ignoreUnknownProps, TypeReference<T> typeReference) throws Exception {
		
		long start = System.nanoTime();
		
		if (StringUtils.isEmpty(xml))
			return null;

		XmlMapper  mapper = new XmlMapper ();

		if (ignoreUnknownProps) {
			mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
			mapper.enable(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL);
		}

		try {
			T res = mapper.readValue(xml, typeReference);
			
			long elapsed = System.nanoTime() - start;
			log.debug("CBS REST | MTMX converter | fromXML, processing time (ms): {}", elapsed/1_000_000);
			
			return res;
		} catch (JsonProcessingException e) {
			log.error("[ERROR] CBS REST | MTMX converter | fromXML, failed for class:" + typeReference, e);
			throw e;
		}
	}

	public <T> String toXML(T object, boolean ifThrow) throws Exception {
		
		long start = System.nanoTime();
		
		if (object == null)
			return null;

		XmlMapper  mapper = new XmlMapper ();

		try {
			String res = mapper.writeValueAsString(object);
			
			long elapsed = System.nanoTime() - start;
			log.debug("CBS REST | MTMX converter | toXML, processing time (ms): {}", elapsed/1_000_000);
			
			return res;
		} catch (JsonProcessingException  e) {
			log.error("[ERROR] CBS REST | MTMX converter | toXML failed for class:" + object.getClass(), e);
			if (ifThrow)
				throw e;

			return null;
		}
	}
}
