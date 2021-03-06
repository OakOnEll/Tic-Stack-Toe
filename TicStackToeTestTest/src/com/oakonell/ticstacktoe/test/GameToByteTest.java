package com.oakonell.ticstacktoe.test;

import java.nio.ByteBuffer;

import junit.framework.TestCase;

import com.oakonell.ticstacktoe.model.Game;
import com.oakonell.ticstacktoe.model.GameMode;
import com.oakonell.ticstacktoe.model.GameType;
import com.oakonell.ticstacktoe.model.Player;
import com.oakonell.ticstacktoe.ui.game.HumanStrategy;
import com.oakonell.ticstacktoe.utils.ByteBufferDebugger;

public class GameToByteTest extends TestCase{
public void testGameToBytes() {
	
	Player blackPlayer = HumanStrategy.createPlayer("Black", true, null);
	Player whitePlayer = HumanStrategy.createPlayer("White", true, null);
	
	Game game = new Game(GameType.STRICT, GameMode.ONLINE, blackPlayer, whitePlayer, blackPlayer);
	
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
