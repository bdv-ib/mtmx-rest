package com.diximonline.mtmx.conv.efts;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.diximonline.conv.IConvToModel;
import com.diximonline.core.CoreException;
import se.highex.ebe.adaptor.connector.pg.PgParamsMtMsg;
import com.diximonline.core.types.ITypeDetectable;
import com.diximonline.core.utils.ITypeDetector;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public enum OperationTypes implements ITypeDetectable {
	
	MT195_AUTH_PMT("195", "pacs.002.001.05", "AUTH;RPMT", new SwfMsgMT195AuthToSwfMsgPacs00200105()),
	MT195_NAUT_PMT("195", "pacs.002.001.05", "NAUT;RPMT", new SwfMsgMT195AuthToSwfMsgPacs00200105()),
	
	NRT("103", "pacs.008.001.04", "NRT", null)
	;
	
	public String mtType;
	public String mxType;
	public String keyWords;
	public IConvToModel<PgParamsMtMsg, PgParamsMtMsg> converter;
	
	static {
		ITypeDetector.init(values());
	}
	
	OperationTypes (String mtType, String mxType, String keyWords, IConvToModel<PgParamsMtMsg, PgParamsMtMsg> converter) {
		this.mtType = mtType;
		this.mxType = mxType;
		this.keyWords = keyWords;
		this.converter = converter;
	}
	
	public static List<String> extractKeyWords(final PgParamsMtMsg msg) {
		
		List<String> actualKW = new ArrayList<String>();
		
		switch (msg.getMsgType()) {
			case "195": {
				Pattern tag75 = Pattern.compile("^:75:(.*)$", Pattern.MULTILINE);
				Matcher m = tag75.matcher(msg.getBlock4());
				String status = m.find() ? m.group(1).trim() : null;
				
				if (status != null) {
					actualKW.add(status);
				}
				
				Pattern tag11S = Pattern.compile("^:11S:(\\d+)", Pattern.MULTILINE);
				m = tag11S.matcher(msg.getBlock4());
				String relatedMsgTypeStr = m.find() ? m.group(1) : null;
				MtTypes relatedMsgType = MtTypes.findByType(relatedMsgTypeStr);
				
				if (relatedMsgType != null && relatedMsgType.isPayment) {
					actualKW.add("RPMT");
				}
			}
		}

		return actualKW;
	}
	
	public static OperationTypes detectOperationType(final String mtType, final List<String> keyWords) {
      
		try {
			return (OperationTypes) ITypeDetector.detect(mtType, keyWords);
		} catch (CoreException e) {
			return null;
		}
	}

	public String getKeyWords() {
		return keyWords;
	}

	@Override
	public String getCode() {
		return name();
	}

	@Override
	public String getIncKeyWords() {
		return keyWords;
	}

	@Override
	public String getExcKeyWords() {
		return "";
	}

	//INFO: can be changed to List to support both mt and mx
	@Override
	public String getNuCode() {
		return mtType;
	}
}
