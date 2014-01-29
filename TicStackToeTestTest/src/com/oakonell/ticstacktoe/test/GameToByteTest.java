package com.oakonell.ticstacktoe.test;

import java.nio.ByteBuffer;

import junit.framework.TestCase;

import com.oakonell.ticstacktoe.model.Game;
import com.oakonell.ticstacktoe.model.Game.ByteBufferDebugger;
import com.oakonell.ticstacktoe.model.GameMode;
import com.oakonell.ticstacktoe.model.GameType;
import com.oakonell.ticstacktoe.model.Player;
import com.oakonell.ticstacktoe.ui.game.HumanStrategy;

public class GameToByteTest extends TestCase{
public void testGameToBytes() {
	
	Player blackPlayer = HumanStrategy.createPlayer("Black", true, null, "123");
	Player whitePlayer = HumanStrategy.createPlayer("White", true, null, "456");
	
	Game game = new Game(GameType.REGULAR, GameMode.ONLINE, blackPlayer, whitePlayer, blackPlayer);
	
	ByteBuffer theBuffer = ByteBuffer.allocate(1024);
	ByteBufferDebugger buffer = new ByteBufferDebugger(theBuffer);
	game.writeBytes("123", buffer);
	int position = theBuffer.position();
	
	byte[] bytes = new byte[position];
	theBuffer.rewind();
	theBuffer.get(bytes);
	
	
	theBuffer= ByteBuffer
			.wrap(bytes);
	
	Game.fromBytes(blackPlayer, whitePlayer, blackPlayer, new ByteBufferDebugger(theBuffer));
	
	
}
}
