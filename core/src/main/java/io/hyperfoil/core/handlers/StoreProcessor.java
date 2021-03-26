package io.hyperfoil.core.handlers;

import org.kohsuke.MetaInfServices;

import io.hyperfoil.api.config.InitFromParam;
import io.hyperfoil.api.config.Name;
import io.hyperfoil.api.processor.Processor;
import io.hyperfoil.api.session.Access;
import io.hyperfoil.core.data.DataFormat;
import io.hyperfoil.core.session.SessionFactory;
import io.netty.buffer.ByteBuf;
import io.hyperfoil.api.session.Session;
import io.hyperfoil.api.session.ResourceUtilizer;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

public class StoreProcessor implements Processor, ResourceUtilizer {
   private static final Logger log = LogManager.getLogger(StoreProcessor.class);

   private final Access toVar;
   private final DataFormat format;

   public StoreProcessor(Access toVar, DataFormat format) {
      this.toVar = toVar;
      this.format = format;
   }

   @Override
   public void before(Session session) {
      toVar.unset(session);
   }

   @Override
   public void process(Session session, ByteBuf data, int offset, int length, boolean isLastPart) {
      ensureDefragmented(isLastPart);
      if (toVar.isSet(session)) {
         log.warn("Variable {} was already set, not setting again.", toVar);
      } else {
         Object value = format.convert(data, offset, length);
         toVar.setObject(session, value);
      }
   }

   @Override
   public void reserve(Session session) {
      toVar.declareObject(session);
   }

   /**
    * Stores data in a session variable (overwriting on multiple occurences).
    */
   @MetaInfServices(Processor.Builder.class)
   @Name("store")
   public static class Builder implements Processor.Builder, InitFromParam<Builder> {
      private Object toVar;
      private DataFormat format = DataFormat.STRING;

      @Override
      public Builder init(String param) {
         this.toVar = param;
         return this;
      }

      /**
       * Variable name.
       *
       * @param toVar Variable name.
       * @return Self.
       */
      public Builder toVar(Object toVar) {
         this.toVar = toVar;
         return this;
      }

      /**
       * Format into which should this processor convert the buffers before storing. Default is <code>STRING</code>.
       *
       * @param format Data format.
       * @return Self.
       */
      public Builder format(DataFormat format) {
         this.format = format;
         return this;
      }

      @Override
      public Processor build(boolean fragmented) {
         StoreProcessor storeProcessor = new StoreProcessor(SessionFactory.access(toVar), format);
         return fragmented ? new DefragProcessor(storeProcessor) : storeProcessor;
      }
   }

   /**
    * DEPRECATED: Use <code>store</code> processor instead.
    */
   @MetaInfServices(Processor.Builder.class)
   @Name("simple")
   public static class LegacyBuilder extends Builder {}
}
