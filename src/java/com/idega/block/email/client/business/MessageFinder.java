package com.idega.block.email.client.business;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.search.FlagTerm;


/**
 * Title:
 * Description:
 * Copyright:    Copyright (c) 2001
 * Company:
 * @author <br><a href="mailto:aron@idega.is">Aron Birkir</a><br>
 * @version 1.0
 */

public class MessageFinder {

  public MessageFinder() {
  }

  public static List<MessageInfo> getMessagesInfo(EmailParams params) {
		try{
      FlagTerm ft = new FlagTerm(new Flags(Flags.Flag.DELETED), false);
      Folder f = params.getFolder();
      Message[] msgs = f.search(ft);
      List<MessageInfo> l = new ArrayList<MessageInfo>();
      MessageInfo mi;
      for (int i = 0; i < msgs.length; i++) {
        mi = new MessageInfo();
        mi.setMessage(msgs[i]);
        l.add(mi);
        System.err.println("msgnum " + mi.getNum());
      }
      return l;
    }
    catch(Exception ex){
      ex.printStackTrace();
    }

    return null;
  }

  public static Map<Integer, MessageInfo> getMappedMessagesInfo(EmailParams params){
    return getMappedMessagesInfo(getMessagesInfo(params));

  }

  public static SortedMap<Integer, MessageInfo> getMappedMessagesInfo(List<MessageInfo> messagesInfo){
    TreeMap<Integer, MessageInfo> m = new TreeMap<Integer, MessageInfo>();
    if(messagesInfo!=null && messagesInfo.size()>0){
      Iterator<MessageInfo> iter = messagesInfo.iterator();
      while (iter.hasNext()) {
        MessageInfo mi = iter.next();
        m.put(new Integer(mi.getNum()),mi);
      }
    }
    return m;
  }
}
