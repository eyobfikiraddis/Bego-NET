package org.example;

import com.mongodb.client.*;
import org.bson.Document;
import io.javalin.Javalin;
import io.javalin.http.Context;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class App {
    private static final String mongoUrl = "mongodb+srv://begonet:oopproject%402024@cluster0.6bghu.mongodb.net/?retryWrites=true&w=majority&appName=Cluster0";
    private static MongoClient mongoClient;
    private static MongoDatabase database;

    public static void main(String[] args) {
        mongoClient = MongoClients.create(mongoUrl);
        database = mongoClient.getDatabase("begonetdb");

        Javalin app = Javalin.create(config -> config.staticFiles.add("/public")).start(7000);

        // Donor submission endpoint
        app.post("/submit-donor", ctx -> {
            String name = ctx.formParam("name");
            String email = ctx.formParam("email");
            String phone = ctx.formParam("phone");
            String address = ctx.formParam("address");
            String item = ctx.formParam("donatedItem");
            String quantity = ctx.formParam("quantity");
            String money = ctx.formParam("donationAmount");

            try {
                MongoCollection<Document> collection = database.getCollection("Potential_D");
                Document donor = new Document("name", name)
                        .append("email", email)
                        .append("phone", phone)
                        .append("address", address)
                        .append("item", item == null || item.isEmpty() ? null : item)
                        .append("quantity", quantity == null || quantity.isEmpty() ? null : quantity)
                        .append("money", money == null || money.isEmpty() ? null : money);

                collection.insertOne(donor);
                ctx.status(201).result("Donor information saved successfully!");
            } catch (Exception e) {
                e.printStackTrace();
                ctx.status(500).result("Failed to save donor information.");
            }
        });

        app.post("/submit-receiver", ctx -> {
            String name = ctx.formParam("name");
            String age = ctx.formParam("age");
            String contact = ctx.formParam("phone");
            String location = ctx.formParam("address");
            String occupation = ctx.formParam("occupation");
            String incomeStr = ctx.formParam("income");
            String account = ctx.formParam("account_number");
            String familySize = ctx.formParam("familySize");
            String roleInFamily = ctx.formParam("role");
            String monetaryAssistance = "";

            try {
                int income = Integer.parseInt(incomeStr);
                if (income > 1000) {
                    ctx.status(400).result("Income exceeds eligibility criteria.");
                } else {
                    if (income > 800) {
                        monetaryAssistance = "3000";
                    } else if (income > 500) {
                        monetaryAssistance = "4000";
                    } else {
                        monetaryAssistance = "5000";
                    }
                }
            } catch (NumberFormatException e) {
                ctx.status(400).result("Invalid income value.");
            }

            try {
                MongoCollection<Document> collection = database.getCollection("Receivers");
                Document receiver = new Document("name", name)
                        .append("contact", contact)
                        .append("location", location)
                        .append("account", account)
                        .append("money", monetaryAssistance);

                collection.insertOne(receiver);
                ctx.status(201).result("Receiver information saved successfully!");
            } catch (Exception e) {
                e.printStackTrace();
                ctx.status(500).result("Failed to save receiver information.");
            }
        });

        app.post("/validate-donation", ctx -> {
            String name = ctx.formParam("name");
            name = name.replaceFirst("^\\s*Name:\\s*", "");

            String email = ctx.formParam("email");
            String phone = ctx.formParam("phone");
            String address = ctx.formParam("address");
            String item = ctx.formParam("item");
            String quantity = ctx.formParam("quantity");
            String moneyStr = ctx.formParam("money");

            if (name == null || name.isEmpty()) {
                ctx.status(400).result("Name is required");
                return;
            }

            Double money = null;
            try {
                money = Double.parseDouble(moneyStr);
            } catch (NumberFormatException e) {
                ctx.status(400).result("Invalid money value. It must be a numeric value.");
                return;
            }

            try {
                boolean isDeleted = deletePotential_d(name.trim());
                if (isDeleted) {
                    insertDonor(name, email, phone, address);
                    insertDonation(item, quantity, String.valueOf(money));
                    ctx.status(200).result("Donation validated and processed successfully.");
                } else {
                    ctx.status(404).result("Donation not found in Potential_D.");
                }
            } catch (Exception e) {
                ctx.status(500).result("Error " + e.getMessage());
                e.printStackTrace();
            }
        });

        app.get("/api/donations", ctx -> {
            try {
                ArrayList<Map<String, Object>> donors = getPotentialDonors();
                ctx.json(donors); // Return the list of donors as JSON
            } catch (Exception e) {
                e.printStackTrace();
                ctx.status(500).result("Failed to fetch donations.");
            }
        });

        // Simple API check
        app.get("/api/data", ctx -> ctx.json(new ResponseMessage("API is working!")));


        HashMap<String, String> userCredentials = new HashMap<>() {{
            put("eyob", "e#y#o#b#");
            put("sekina", "oopproject2024");
            put("misgana", "misgana");
            put("eyuel", "eul_zzz");
            put("betel", "ben_2122");
            put("leoul", "llll_3434");
        }};

        // Login endpoint
        app.post("/login", ctx -> handleLogin(ctx, userCredentials));

        app.get("/api/donors", ctx -> {
            try {
                ctx.json(getDonors());
            } catch (Exception e) {
                ctx.status(500).result("Failed to fetch donor data.");
            }
        });

        app.get("/api/items", ctx -> {
            try {
                ctx.json(getDonations());
            } catch (Exception e) {
                ctx.status(500).result("Failed to fetch item data.");
            }
        });
    }

    public static ArrayList<Map<String, Object>> getPotentialDonors() {
        ArrayList<Map<String, Object>> potentialDonors = new ArrayList<>();
        MongoCollection<Document> collection = database.getCollection("Potential_D");

        try (MongoCursor<Document> cursor = collection.find().iterator()) {
            while (cursor.hasNext()) {
                Document doc = cursor.next();
                Map<String, Object> donor = new HashMap<>();
                donor.put("Name", doc.getString("name"));
                donor.put("Email", doc.getString("email"));
                donor.put("Phone", doc.getString("phone"));
                donor.put("Address", doc.getString("address"));
                donor.put("Item", doc.getString("item"));
                donor.put("Quantity", doc.getString("quantity"));
                donor.put("Money", doc.getString("money"));
                potentialDonors.add(donor);
            }
        }
        return potentialDonors;
    }

    public static boolean deletePotential_d(String name) {
        MongoCollection<Document> collection = database.getCollection("Potential_D");
        long deletedCount = collection.deleteOne(new Document("name", name)).getDeletedCount();
        return deletedCount > 0;
    }

    public static void insertDonor(String name, String email, String phone, String address) {
        MongoCollection<Document> collection = database.getCollection("Donors");
        Document donor = new Document("name", name)
                .append("email", email)
                .append("phone", phone)
                .append("address", address);

        collection.insertOne(donor);
    }

    public static void insertDonation(String item, String quantity, String money) {
        MongoCollection<Document> collection = database.getCollection("Donations");
        Document donation = new Document("item", item)
                .append("quantity", quantity)
                .append("money", money);

        collection.insertOne(donation);
    }

    private static void handleLogin(Context ctx, HashMap<String, String> userCredentials) {
        String username = ctx.formParam("username");
        String password = ctx.formParam("password");

        if (username == null || password == null) {
            ctx.status(400).result("Username and password are required!");
            return;
        }

        if (userCredentials.getOrDefault(username, "").equals(password)) {
            ctx.redirect("/admin.html");
        } else {
            ctx.status(401).result("Invalid username or password.");
        }
    }


    static class ResponseMessage {
        private String message;

        public ResponseMessage(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }

    public static Map<String, Integer> getDonations() {
        MongoCollection<Document> collection = database.getCollection("Donations");
        Map<String, Integer> items = new HashMap<>();

        try (MongoCursor<Document> cursor = collection.find().iterator()) {
            while (cursor.hasNext()) {
                Document doc = cursor.next();
                String item = doc.getString("item");
                int quantity = doc.getInteger("quantity", 0);
                items.put(item, items.getOrDefault(item, 0) + quantity);
            }
        }
        return items;
    }
    public static ArrayList<Map<String, Object>> getDonors() {
        ArrayList<Map<String, Object>> donors = new ArrayList<>();
        MongoCollection<Document> collection = database.getCollection("Donors");

        try (MongoCursor<Document> cursor = collection.find().iterator()) {
            while (cursor.hasNext()) {
                Document doc = cursor.next();
                Map<String, Object> donor = new HashMap<>();
                donor.put("Name", doc.getString("name"));
                donor.put("Email", doc.getString("email"));
                donor.put("Phone", doc.getString("phone"));
                donor.put("Address", doc.getString("address"));
                donors.add(donor);
            }
        }
        return donors;
    }

}
