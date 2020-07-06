package io.pivotal.services.dataTx.geode.serialization;

import io.pivotal.services.dataTx.geode.demo.ComplexObject;
import io.pivotal.services.dataTx.geode.demo.SimpleObject;
import io.pivotal.services.dataTx.geode.serialization.exception.InvalidSerializationKeyException;
import nyla.solutions.core.security.user.data.UserProfile;
import nyla.solutions.core.util.Organizer;
import org.apache.geode.cache.Cache;
import org.apache.geode.cache.CacheClosedException;
import org.apache.geode.cache.CacheFactory;
import org.apache.geode.cache.Region;
import org.apache.geode.pdx.PdxInstance;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mockito;

import java.io.IOException;
import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.*;

//@Disabled
public class PDXTest
{

	@BeforeEach
	public void setUp()
	throws Exception
	{
		Cache cache = null;


		try {
			cache = CacheFactory.getAnyInstance();

		}
		catch(CacheClosedException e) {
			if(cache == null)
				cache = new CacheFactory().create();
		}
		catch(Exception e)
		{
			//should already have factory
		}
	}//-------------------------------------------

	@Test
	public void test_convert_from_json_to_pdxInstance()
	throws Exception
	{
		UserProfile expected = new UserProfile();
		expected.setEmail("user");

		String json = PDX.toJsonFromNondPdxObject(expected);

		System.out.println("json:"+json);

		assertTrue(json != null && json.length() > 0);

		assertTrue(json.contains("@type"));

		PdxInstance pdx = PDX.fromJSON(json);

		UserProfile actual = (UserProfile)pdx.getObject();
		assertEquals(expected,actual);


	}

	@Test
	public void reject_invalid_json()
	throws IOException
	{
		String invalid = "{\"sdsdfdfsf "+PDX.JSON_TYPE_ATTRIBUTE+"}";


		try
		{
			PdxInstance instance = PDX.fromJSON(invalid);
			fail("invalid JSON");
		}
		catch(IllegalArgumentException e){
			System.out.println("exception:"+e);
			assertTrue(e.getMessage().contains(invalid));
		}

	}//-------------------------------------------

	@Test
	public void reject_valid_json_with_missing_type()
	throws IOException
	{
		String invalid = "{\"email\": 1223, \"firstName\": \"testFirst\"}";


		try
			{
			PdxInstance instance = PDX.fromJSON(invalid);
			fail("Must catch missing type");
		}
		catch(IllegalArgumentException e){
			e.printStackTrace();
			System.out.println("exception:"+e);
			assertTrue(e.getMessage().contains(PDX.JSON_TYPE_ATTRIBUTE));
		}

	}//-------------------------------------------
	@Test
	public void testFromJSON_Complex()
	throws Exception
	{

		ComplexObject expected = new ComplexObject();
		SimpleObject simpleObject = new SimpleObject();
		simpleObject.setFieldInt(1);
		simpleObject.setBigDecimal(new BigDecimal(232));
		simpleObject.setFiedByte( Byte.valueOf("2"));
		simpleObject.setFieldBoolean(true);
		simpleObject.setFieldChar('e');
		simpleObject.setFieldLongObject(Long.valueOf(223));
		simpleObject.setFieldShortObject(Short.valueOf("2"));
		//TODO: simpleObject.setLocalDateTime(LocalDateTime.now()); //DEFECT
		//TODO: simpleObject.setLocalDate(LocalDate.now()); //DEFECT
		//TODO: simpleObject.setLocalTime(LocalTime.now()); //DEFECT
		//TODO: simpleObject.setFieldTime(new Time(Calendar.getInstance().getTime().getTime())); //DEFECT
		//TODO: simpleObject.setException(new Exception("Sdsds")); //DEFECT
		//TODO: simpleObject.setError(new Error("Sdsd")); //DEFECT
		//TODO: simpleObject.setFieldCalendar(Calendar.getInstance()); //DEFECT
		simpleObject.setFieldClass(PDXTest.class);
		simpleObject.setFieldDate(Calendar.getInstance().getTime());
		simpleObject.setFieldTimestamp(new Timestamp(Calendar.getInstance().getTimeInMillis()));

		expected.setSimpleObject((SimpleObject)simpleObject.clone());
		expected.setMap(new HashMap<String,String>());
		expected.getMap().put("sdsd","Sdsds");
		ComplexObject[] arrays = {(ComplexObject)expected.clone()};
		expected.setComplexArray(arrays);
		expected.setComplexObject((ComplexObject)expected.clone());


		ComplexObject duplicate = (ComplexObject)expected.clone();
		assertEquals(duplicate,expected);

		String json = PDX.toJsonFromNondPdxObject(expected);


		PdxInstance pdx = PDX.fromJSON(json);
		
		assertNotNull(pdx);

		ComplexObject actual = (ComplexObject)pdx.getObject();

		assertEquals(expected,actual);
	}//-------------------------------------------
	@Test
	public void test_PDX_instance_to_json()
	throws Exception {
		UserProfile userProfile = new UserProfile();
		userProfile.setEmail("a@pivotal.io");

		String json = PDX.toJsonFromNondPdxObject(userProfile);
		System.out.println("JSON:"+json);

		PdxInstance pdx = PDX.fromJSON(json);

		String actual = PDX.toJSON(
				pdx,UserProfile.class.getName());
		assertTrue(actual.contains("a@pivotal.io"));
		assertTrue(actual.contains("@type"));
		assertTrue(actual.contains(userProfile.getClass().getName()));
	}//-------------------------------------------
	@Test
	public void test_region_to_jsonMapEntry()
	throws Exception
	{
		BigDecimal expectedKey = new BigDecimal("25");
		PdxInstance pdxInstance = PDX.fromObject(new ComplexObject());

		Set<Serializable> expectedKeys = Organizer.toSet(expectedKey);
		Region<Serializable,PdxInstance> region = Mockito.mock(Region.class);
		Mockito.when(region.keySetOnServer()).thenReturn(expectedKeys);
		Mockito.when(region.get(Mockito.any())).thenReturn(pdxInstance);

		Collection<Serializable> keys = region.keySetOnServer();

		SerializationPdxEntryWrapper wrapper;

		for (Serializable key: keys)
		{
			wrapper = PDX.toSerializePdxEntryWrapper(key,ComplexObject.class.getName(),region.get(key));
			assertNotNull(wrapper);
			assertEquals(expectedKey.getClass().getName(),wrapper.getKeyClassName());
			assertEquals(key,wrapper.deserializeKey());
			assertEquals(region.get(key),wrapper.toPdxInstance());

		}
	}

	@Test
	public void wrapper_json()
	throws IOException
	{
		Long keyLong = 12L;
		PdxInstance pdx = PDX.fromObject(new UserProfile());
		SerializationPdxEntryWrapper expected = new SerializationPdxEntryWrapper(keyLong,
				UserProfile.class.getName(),pdx);

		String json = PDX.toJsonFromNondPdxObject(expected);
		assertTrue(!json.contains(SerializationPdxEntryWrapper.class.getName()));

		SerializationPdxEntryWrapper actual = PDX.toSerializePdxEntryWrapperFromJson(json);
		assertEquals(expected,actual);

		Double keyDouble = 12.0;
		expected = new SerializationPdxEntryWrapper(keyLong,UserProfile.class.getName(),pdx);
		json = PDX.toJsonFromNondPdxObject(expected);
		actual = PDX.toSerializePdxEntryWrapperFromJson(json);
		assertEquals(expected,actual);

		BigDecimal keyBigDecimal = BigDecimal.TEN;
		expected = new SerializationPdxEntryWrapper(keyBigDecimal,UserProfile.class.getName(),pdx);
		json = PDX.toJsonFromNondPdxObject(expected);
		actual = PDX.toSerializePdxEntryWrapperFromJson(json);
		assertEquals(expected,actual);


		String keystring = "sdsdsd";
		expected = new SerializationPdxEntryWrapper(keystring,UserProfile.class.getName(),pdx);
		json = PDX.toJsonFromNondPdxObject(expected);
		actual = PDX.toSerializePdxEntryWrapperFromJson(json);
		assertEquals(expected,actual);


		try
		{

			UserProfile invalid = new UserProfile();
			expected = new SerializationPdxEntryWrapper(invalid,UserProfile.class.getName(),pdx);
			fail("Invalid key");
		}
		catch(InvalidSerializationKeyException e){

		}



	}
}
