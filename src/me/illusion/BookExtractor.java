package me.illusion;

import eisenwave.nbt.NBTCompound;
import eisenwave.nbt.NBTList;
import eisenwave.nbt.NBTNamedTag;
import eisenwave.nbt.NBTTag;
import eisenwave.nbt.io.NBTDeserializer;
import me.illusion.parser.RegionFile;
import me.illusion.parser.SimpleNBTReader;

import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class BookExtractor {

    // List of books, used for duplicate checks
    private final List<WrittenBook> books = new ArrayList<>();

    // Supported book materials
    private static final List<String> SUPPORTED_BOOKS = Arrays.asList(
            "minecraft:written_book",
            "minecraft:writable_book"
    );

    private BookExtractor(String[] args) {
        // Obtains location of jarfile
        File jarFile = new File(BookExtractor.class.getProtectionDomain().getCodeSource().getLocation().getPath());
        // Obtains world folder
        File folder = new File(jarFile.getParentFile() + File.separator + args[0]);

        // Obtains player data folder
        File dataFolder = new File(folder + File.separator + "playerdata");

        File[] playerFiles = dataFolder.listFiles();

        // Handles player files
        for (File file : playerFiles)
            handlePlayerFile(file);

        // Obtains region folder
        File regionFolder = new File(folder + File.separator + "region");

        File[] regionFiles = regionFolder.listFiles();

        // Handles region files
        for (File file : regionFiles) {
            try {
                handleRegionFile(file);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Creates books from region file
     *
     * @param file - The .MCA file
     * @throws Exception if any IO issues are found
     */
    private void handleRegionFile(File file) throws Exception {
        RegionFile region = new RegionFile(file); // Creates a RegionFile instance

        for (int x = 0; x < 32; x++) // Loops through chunks in the X axis
            for (int z = 0; z < 32; z++) { // Loops through chunks in the Y axis
                if (!region.hasChunk(x, z)) // Makes sure region has the chunk
                    continue;

                DataInputStream stream = region.getChunkDataInputStream(x, z); // Gets the DataInputStream for the specific chunk

                SimpleNBTReader.Node rootNode = (SimpleNBTReader.Node) SimpleNBTReader.read(stream); // Obtains the rood node

                List<SimpleNBTReader.Node> root = (List<SimpleNBTReader.Node>) rootNode.getValue(); // Obtains all the nodes in the root

                List<SimpleNBTReader.Node> level = (List<SimpleNBTReader.Node>) getObject("Level", root); // Obtains the Level node in the root

                Object[] tiles = (Object[]) getObject("TileEntities", level); // From the level, it obtains the TileEntities

                if (tiles == null || tiles.length == 0) // If the tile entities aren't present, don't go further
                    continue;

                // Converts tiles[] into a list of node lists
                List<List<SimpleNBTReader.Node>> tileList = Stream.of(tiles).map(tile -> (LinkedList<SimpleNBTReader.Node>) tile).collect(Collectors.toList());

                for (List<SimpleNBTReader.Node> entry : tileList) { // Iterates through each nodelist in the tile entities

                    Object[] items = (Object[]) getObject("Items", entry); // Obtains the Items section

                    if (items == null) // If the items section isn't present (isn't container)
                        continue; // Don't go further

                    // Converts items[] into list of node lists
                    List<List<SimpleNBTReader.Node>> itemList = Stream.of(items).map((item) -> (List<SimpleNBTReader.Node>) item).collect(Collectors.toList());

                    for (List<SimpleNBTReader.Node> item : itemList) { // Iterates through each nodelist in the items
                        if (!isBook(item)) // If the nodelist isn't part of a book
                            continue; // Don't go further

                        // Obtains the tag, where all the book data is stored
                        List<SimpleNBTReader.Node> tag = (List<SimpleNBTReader.Node>) getObject("tag", item);

                        if (tag == null) // If the tag isn't present (book isn't written to yet)
                            continue;

                        if (getObject("generation", tag) != null) // If the book is a duplicate (Copy of a Copy, Copy of Original)
                            continue;

                        String author = (String) getObject("author", tag); // Obtains author

                        if (author == null)
                            author = "unknown";

                        String title = (String) getObject("title", tag); // Obtains title

                        if (title == null)
                            title = "unknown";

                        Object[] pages = (Object[]) getObject("pages", tag); // Obtains pages (UNICODE NOT PRESENT???)

                        // Converts pages to StringList
                        List<String> text = Stream.of(pages).map(page -> (String) page).collect(Collectors.toList());

                        // Checks if the text doesn't exist already
                        if (canAdd(text))
                            books.add(new WrittenBook(author, title, text));
                    }

                }

                stream.close();
            }


        region.close();

    }

    /**
     * Creates books from .DAT file, found in playerdata
     *
     * @param file - The .DAT file
     */
    private void handlePlayerFile(File file) {
        try {
            NBTNamedTag tag = new NBTDeserializer().fromFile(file);

            NBTCompound root = (NBTCompound) tag.getTag();

            handle(root.getTagList("Inventory"));
            handle(root.getTagList("EnderItems"));

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Iterates through NBTList, creating all books possible,
     * used for DAT files as .MCA aren't legible by the lib
     *
     * @param list - The list of NBT compounds to work on
     */
    private void handle(NBTList list) {
        for (NBTTag nbttag : list) {
            NBTCompound compound = (NBTCompound) nbttag;

            if (!SUPPORTED_BOOKS.contains(compound.getString("id")))
                continue;

            NBTCompound tag = compound.getCompoundTag("tag");

            String author = tag.hasKey("author") ? tag.getString("author") : "unknown";
            String name = tag.hasKey("title") ? tag.getString("title") : "unknown";
            List<String> text = new ArrayList<>();

            for (NBTTag page : tag.getTagList("pages")) {
                String pageText = page.toString();
                text.add(pageText.substring(1, pageText.length() - 1).replace("\\", ""));
            }

            if (canAdd(text))
                books.add(new WrittenBook(author, name, text));
        }
    }

    /**
     * Iterates through nodes, checks if item is a book
     *
     * @param nodes - The nodes to check
     * @return TRUE if item is a writable/written book, FALSE otherwise
     */
    private boolean isBook(List<SimpleNBTReader.Node> nodes) {
        for (SimpleNBTReader.Node node : nodes)
            if (node.getName().equalsIgnoreCase("id"))
                return SUPPORTED_BOOKS.contains(node.getValue().toString());

        return false;
    }

    /**
     * Iterates through books, compares duplicate text
     *
     * @param text - The text to check
     * @return TRUE if text is not duplicate, FALSE otherwise
     */
    private boolean canAdd(List<String> text) {
        for (WrittenBook book : books)
            if (book.getText().equals(text))
                return false;
        return true;
    }

    /**
     * Fetches an object from node ID
     *
     * @param name - The object name
     * @param list - The node list
     * @return NULL if object not found, object otherwise
     */
    private Object getObject(String name, List<SimpleNBTReader.Node> list) {
        return getObject(name, list.toArray(new SimpleNBTReader.Node[]{}));
    }

    /**
     * Fetches an object from node ID
     *
     * @param name  - The object name
     * @param nodes - The nodes to filter from
     * @return NULL if object not found, object otherwise
     */
    private Object getObject(String name, SimpleNBTReader.Node... nodes) {
        for (SimpleNBTReader.Node node : nodes) {
            if (node.getName().equalsIgnoreCase(name))
                return node.getValue();
        }

        return null;
    }

    public static void main(String[] args) {
        new BookExtractor(args);
    }

}
