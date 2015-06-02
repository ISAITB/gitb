package com.gitb.types;

import com.gitb.core.ErrorCode;
import com.gitb.exceptions.GITBEngineInternalError;
import com.gitb.utils.ErrorUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by tuncay on 9/25/14.
 */
public abstract class PrimitiveType extends DataType{
    private static Logger logger = LoggerFactory.getLogger(PrimitiveType.class);
    public static final String DEFAULT_ENCODING = "utf-8";

    @Override
    public void deserialize(byte[] content) {
        deserialize(content, PrimitiveType.DEFAULT_ENCODING);
    }

    @Override
    public void deserialize(InputStream inputStream){
        deserialize(inputStream, PrimitiveType.DEFAULT_ENCODING);
    }

    @Override
    public void deserialize(InputStream inputStream, String encoding) {
	    try {
		    deserialize(IOUtils.toByteArray(inputStream), encoding);
	    } catch (IOException e) {
		    throw new GITBEngineInternalError(ErrorUtils.errorInfo(ErrorCode.DATATYPE_ERROR, "An error occurred during deserialization."), e);
	    }
    }

    @Override
    public OutputStream serializeToStream(String encoding) {
	    try {
		    byte [] content = serialize(encoding);
		    OutputStream os = new ByteArrayOutputStream();
		    os.write(content);

		    return os;
	    } catch (IOException e) {
            throw new GITBEngineInternalError(ErrorUtils.errorInfo(ErrorCode.DATATYPE_ERROR, "An error occurred during serialization."), e);
	    }
    }

    @Override
    public OutputStream serializeToStreamByDefaultEncoding() {
        return serializeToStream(PrimitiveType.DEFAULT_ENCODING);
    }

    @Override
    public byte[] serializeByDefaultEncoding() {
        return serialize(PrimitiveType.DEFAULT_ENCODING);
    }

}
