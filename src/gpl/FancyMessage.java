package gpl;

import com.avrgaming.civcraft.util.Reflection;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.server.v1_12_R1.PacketPlayOutChat;
import net.minecraft.server.v1_12_R1.IChatBaseComponent.ChatSerializer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.craftbukkit.v1_12_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class FancyMessage {
   @SuppressWarnings({ "unchecked", "rawtypes" })
private List<FancyMessage.MessagePart> messageParts = new ArrayList();

   public FancyMessage(String firstPartText) {
      this.messageParts.add(new FancyMessage.MessagePart(firstPartText));
   }

   public FancyMessage color(ChatColor color) {
      if (!color.isColor()) {
         throw new IllegalArgumentException(color.name() + " is not a color");
      } else {
         this.latest().color = color;
         return this;
      }
   }

   public FancyMessage style(ChatColor... styles) {
      ChatColor[] var5 = styles;
      int var4 = styles.length;

      for(int var3 = 0; var3 < var4; ++var3) {
         ChatColor style = var5[var3];
         if (!style.isFormat()) {
            throw new IllegalArgumentException(style.name() + " is not a style");
         }
      }

      this.latest().styles = styles;
      return this;
   }

   public FancyMessage file(String path) {
      this.onClick("open_file", path);
      return this;
   }

   public FancyMessage link(String url) {
      this.onClick("open_url", url);
      return this;
   }

   public FancyMessage suggest(String command) {
      this.onClick("suggest_command", command);
      return this;
   }

   public FancyMessage command(String command) {
      this.onClick("run_command", command);
      return this;
   }

   public FancyMessage tooltip(String text) {
      this.onHover("show_text", text);
      return this;
   }

public FancyMessage itemTooltip(ItemStack itemStack) {
      try {
         Object nmsItem = Reflection.getMethod(Reflection.getOBCClass("inventory.CraftItemStack"), "asNMSCopy", ItemStack.class).invoke((Object)null, itemStack);
         return this.itemTooltip(Reflection.getMethod(Reflection.getNMSClass("ItemStack"), "save", Reflection.getNMSClass("NBTTagCompound")).invoke(nmsItem, Reflection.getNMSClass("NBTTagCompound").newInstance()).toString());
      } catch (Exception var3) {
         var3.printStackTrace();
         return this;
      }
   }

   public FancyMessage itemTooltip(String itemJSON) {
      this.onHover("show_item", itemJSON);
      return this;
   }

   public FancyMessage then(Object obj) {
      this.messageParts.add(new FancyMessage.MessagePart(obj.toString()));
      return this;
   }

   public String toJSONString() {
      StringWriter stringWriter = new StringWriter();
      JsonWriter json = new JsonWriter(stringWriter);

      try {
         if (this.messageParts.size() == 1) {
            this.latest().writeJson(json);
         } else {
            json.beginObject().name("text").value("").name("extra").beginArray();
            @SuppressWarnings("rawtypes")
			Iterator var4 = this.messageParts.iterator();

            while(var4.hasNext()) {
               FancyMessage.MessagePart part = (FancyMessage.MessagePart)var4.next();
               part.writeJson(json);
            }

            json.endArray().endObject();
         }
      } catch (IOException var5) {
         throw new RuntimeException("invalid message");
      }

      return stringWriter.toString();
   }

   public void send(Player player) {
      ((CraftPlayer)player).getHandle().playerConnection.sendPacket(new PacketPlayOutChat(ChatSerializer.a(this.toJSONString())));
   }

   public void sendAll() {
      @SuppressWarnings("rawtypes")
	Iterator var1 = Bukkit.getOnlinePlayers().iterator();

      while(var1.hasNext()) {
         Player player = (Player)var1.next();
         this.send(player);
      }

   }

   private FancyMessage.MessagePart latest() {
      return (FancyMessage.MessagePart)this.messageParts.get(this.messageParts.size() - 1);
   }

   private void onClick(String name, String data) {
      FancyMessage.MessagePart latest = this.latest();
      latest.clickActionName = name;
      latest.clickActionData = data;
   }

   private void onHover(String name, String data) {
      FancyMessage.MessagePart latest = this.latest();
      latest.hoverActionName = name;
      latest.hoverActionData = data;
   }

   static class MessagePart {
      public final String text;
      public ChatColor color = null;
      public ChatColor[] styles = null;
      public String clickActionName = null;
      public String clickActionData = null;
      public String hoverActionName = null;
      public String hoverActionData = null;

      public MessagePart(String text) {
         this.text = text;
      }

      public JsonWriter writeJson(JsonWriter json) throws IOException {
         json.beginObject().name("text").value(this.text);
         if (this.color != null) {
            json.name("color").value(this.color.name().toLowerCase());
         }

         if (this.styles != null) {
            ChatColor[] var5 = this.styles;
            int var4 = this.styles.length;

            for(int var3 = 0; var3 < var4; ++var3) {
               ChatColor style = var5[var3];
               json.name(style == ChatColor.UNDERLINE ? "underlined" : style.name().toLowerCase()).value(true);
            }
         }

         if (this.clickActionName != null && this.clickActionData != null) {
            json.name("clickEvent").beginObject().name("action").value(this.clickActionName).name("value").value(this.clickActionData).endObject();
         }

         if (this.hoverActionName != null && this.hoverActionData != null) {
            json.name("hoverEvent").beginObject().name("action").value(this.hoverActionName).name("value").value(this.hoverActionData).endObject();
         }

         return json.endObject();
      }
   }
}
    