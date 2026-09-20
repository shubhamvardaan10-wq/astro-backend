package com.astro.service;

import com.astro.model.CityInfo;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Provides lat/lon for Indian cities.
 * All Indian cities share IST (UTC+5:30); no DST.
 */
@Service
public class CityService {

    private final Map<String, CityInfo> cities = new LinkedHashMap<>();

    public CityService() {
        add("Mumbai",           19.0760,  72.8777);
        add("Delhi",            28.6139,  77.2090);
        add("New Delhi",        28.6139,  77.2090);
        add("Bangalore",        12.9716,  77.5946);
        add("Bengaluru",        12.9716,  77.5946);
        add("Chennai",          13.0827,  80.2707);
        add("Kolkata",          22.5726,  88.3639);
        add("Calcutta",         22.5726,  88.3639);
        add("Hyderabad",        17.3850,  78.4867);
        add("Pune",             18.5204,  73.8567);
        add("Ahmedabad",        23.0225,  72.5714);
        add("Jaipur",           26.9124,  75.7873);
        add("Lucknow",          26.8467,  80.9462);
        add("Kanpur",           26.4499,  80.3319);
        add("Nagpur",           21.1458,  79.0882);
        add("Surat",            21.1702,  72.8311);
        add("Indore",           22.7196,  75.8577);
        add("Bhopal",           23.2599,  77.4126);
        add("Patna",            25.5941,  85.1376);
        add("Hajipur",          25.6858,  85.2146);
        add("Vadodara",         22.3072,  73.1812);
        add("Agra",             27.1767,  78.0081);
        add("Varanasi",         25.3176,  82.9739);
        add("Meerut",           28.9845,  77.7064);
        add("Visakhapatnam",    17.6868,  83.2185);
        add("Vizag",            17.6868,  83.2185);
        add("Coimbatore",       11.0168,  76.9558);
        add("Rajkot",           22.3039,  70.8022);
        add("Ludhiana",         30.9010,  75.8573);
        add("Amritsar",         31.6340,  74.8723);
        add("Nashik",           19.9975,  73.7898);
        add("Faridabad",        28.4089,  77.3178);
        add("Ghaziabad",        28.6692,  77.4538);
        add("Ranchi",           23.3441,  85.3096);
        add("Chandigarh",       30.7333,  76.7794);
        add("Thiruvananthapuram", 8.5241, 76.9366);
        add("Trivandrum",        8.5241,  76.9366);
        add("Kochi",             9.9312,  76.2673);
        add("Cochin",            9.9312,  76.2673);
        add("Guwahati",         26.1445,  91.7362);
        add("Bhubaneswar",      20.2961,  85.8245);
        add("Raipur",           21.2514,  81.6296);
        add("Jodhpur",          26.2389,  73.0243);
        add("Madurai",           9.9252,  78.1198);
        add("Vijayawada",       16.5062,  80.6480);
        add("Dehradun",         30.3165,  78.0322);
        add("Shimla",           31.1048,  77.1734);
        add("Srinagar",         34.0837,  74.7973);
        add("Jammu",            32.7266,  74.8570);
        add("Mysuru",           12.2958,  76.6394);
        add("Mysore",           12.2958,  76.6394);
        add("Thane",            19.2183,  72.9781);
        add("Noida",            28.5355,  77.3910);
        add("Gurugram",         28.4595,  77.0266);
        add("Gurgaon",          28.4595,  77.0266);
        add("Prayagraj",        25.4358,  81.8463);
        add("Allahabad",        25.4358,  81.8463);
        add("Tirupati",         13.6288,  79.4192);
        add("Haridwar",         29.9457,  78.1642);
        add("Rishikesh",        30.0869,  78.2676);
        add("Udaipur",          24.5854,  73.7125);
        add("Kolhapur",         16.6950,  74.2083);
        add("Aurangabad",       19.8762,  75.3433);
        add("Solapur",          17.6800,  75.9064);
        add("Jabalpur",         23.1815,  79.9864);
        add("Gwalior",          26.2183,  78.1828);
        add("Kota",             25.2138,  75.8648);
        add("Bikaner",          28.0229,  73.3119);
        add("Bhilai",           21.2090,  81.4285);
        add("Mangalore",        12.9141,  74.8560);
        add("Hubli",            15.3647,  75.1240);
        add("Belgaum",          15.8497,  74.4977);
        add("Salem",            11.6643,  78.1460);
        add("Tiruchirappalli",  10.7905,  78.7047);
        add("Trichy",           10.7905,  78.7047);
    }

    private void add(String name, double lat, double lon) {
        cities.put(name.toLowerCase(Locale.ROOT), new CityInfo(name, lat, lon));
    }

    /** Case-insensitive city lookup. Returns null if not found. */
    public CityInfo findCity(String name) {
        if (name == null) return null;
        return cities.get(name.trim().toLowerCase(Locale.ROOT));
    }

    /** All city names (canonical). */
    public List<String> listCities() {
        return cities.values().stream().map(CityInfo::getName).toList();
    }
}
