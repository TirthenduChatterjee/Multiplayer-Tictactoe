import React, { useEffect, useMemo, useRef, useState } from 'react';
import { createRoot } from 'react-dom/client';
import { Copy, LogIn, Plus, RotateCcw, Sparkles, Users } from 'lucide-react';
import './styles.css';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080';
const WS_BASE_URL = import.meta.env.VITE_WS_BASE_URL ?? 'ws://localhost:8080';

function App() {
  const [playerName, setPlayerName] = useState('');
  const [roomCodeInput, setRoomCodeInput] = useState('');
  const [session, setSession] = useState(() => readSession());
  const [room, setRoom] = useState(null);
  const [notice, setNotice] = useState('');
  const [busy, setBusy] = useState(false);
  const [connected, setConnected] = useState(false);
  const socketRef = useRef(null);

  useEffect(() => {
    if (!session?.code) {
      return undefined;
    }

    const socket = new WebSocket(`${WS_BASE_URL}/ws/rooms/${session.code}`);
    socketRef.current = socket;

    socket.onopen = () => setConnected(true);
    socket.onclose = () => setConnected(false);
    socket.onerror = () => setNotice('Realtime connection failed. Refresh once the backend is running.');
    socket.onmessage = (event) => {
      const message = JSON.parse(event.data);
      if (message.type === 'ROOM_STATE') {
        setRoom(message.payload);
        setNotice(message.payload.message ?? '');
      }
      if (message.type === 'ERROR') {
        setNotice(message.payload.message);
      }
    };

    return () => socket.close();
  }, [session]);

  useEffect(() => {
    if (session) {
      localStorage.setItem('tic-tac-toe-session', JSON.stringify(session));
    } else {
      localStorage.removeItem('tic-tac-toe-session');
    }
  }, [session]);

  const statusText = useMemo(() => {
    if (!room) {
      return 'Setting the board...';
    }
    if (room.status === 'WAITING') {
      return 'Waiting for your friend to join';
    }
    if (room.status === 'DRAW') {
      return 'Draw. Perfectly balanced.';
    }
    if (room.status === 'X_WON') {
      return `${room.xPlayer.name} wins`;
    }
    if (room.status === 'O_WON') {
      return `${room.oPlayer?.name ?? 'O'} wins`;
    }
    return `${room.nextTurn}'s turn`;
  }, [room]);

  async function createRoom() {
    await submit(async () => {
      const data = await api('/api/rooms', {
        method: 'POST',
        body: { playerName }
      });
      setSession({ code: data.room.code, symbol: data.symbol, playerName: playerName.trim() });
      setRoom(data.room);
    });
  }

  async function joinRoom() {
    await submit(async () => {
      const code = roomCodeInput.trim().toUpperCase();
      const data = await api(`/api/rooms/${code}/join`, {
        method: 'POST',
        body: { playerName }
      });
      setSession({ code: data.room.code, symbol: data.symbol, playerName: playerName.trim() });
      setRoom(data.room);
    });
  }

  async function submit(action) {
    if (!playerName.trim()) {
      setNotice('Enter your name first.');
      return;
    }
    setBusy(true);
    setNotice('');
    try {
      await action();
    } catch (error) {
      setNotice(error.message);
    } finally {
      setBusy(false);
    }
  }

  function playCell(index) {
    if (!room || !session || room.board[index] !== '-' || room.status !== 'IN_PROGRESS') {
      return;
    }
    socketRef.current?.send(JSON.stringify({
      type: 'MOVE',
      cellIndex: index,
      playerName: session.playerName
    }));
  }

  function restart() {
    socketRef.current?.send(JSON.stringify({
      type: 'RESTART',
      playerName: session.playerName
    }));
  }

  function leaveRoom() {
    setSession(null);
    setRoom(null);
    setConnected(false);
    setNotice('');
  }

  function copyCode() {
    navigator.clipboard?.writeText(session.code);
    setNotice('Room code copied.');
  }

  if (!session) {
    return (
      <main className="shell">
        <section className="entry-panel">
          <div className="brand-row">
            <span className="brand-mark"><Sparkles size={19} /></span>
            <span>Neon Toe</span>
          </div>
          <h1>Online Tic Tac Toe</h1>
          <p className="lede">Create a private room, send the code, and play live on a sharp dark board.</p>

          <label>
            Your name
            <input
              value={playerName}
              onChange={(event) => setPlayerName(event.target.value)}
              placeholder="Tirtho"
              maxLength={40}
            />
          </label>

          <div className="actions-grid">
            <button className="primary-action" onClick={createRoom} disabled={busy}>
              <Plus size={19} />
              Create Room
            </button>
            <div className="join-row">
              <input
                value={roomCodeInput}
                onChange={(event) => setRoomCodeInput(event.target.value.toUpperCase())}
                placeholder="ROOM CODE"
                maxLength={6}
              />
              <button onClick={joinRoom} disabled={busy || !roomCodeInput.trim()} aria-label="Join room">
                <LogIn size={19} />
              </button>
            </div>
          </div>

          {notice && <p className="notice">{notice}</p>}
        </section>
      </main>
    );
  }

  return (
    <main className="game-shell">
      <section className="topbar">
        <div>
          <div className="brand-row compact">
            <span className="brand-mark"><Sparkles size={17} /></span>
            <span>Neon Toe</span>
          </div>
          <h1>{statusText}</h1>
        </div>
        <div className="room-tools">
          <button className="code-button" onClick={copyCode}>
            <Copy size={17} />
            {session.code}
          </button>
          <button className="icon-button" onClick={restart} aria-label="Restart game">
            <RotateCcw size={18} />
          </button>
        </div>
      </section>

      <section className="arena">
        <div className="board-wrap">
          <div className="board" aria-label="Tic tac toe board">
            {(room?.board ?? Array(9).fill('-')).map((cell, index) => (
              <button
                className={`cell ${cell !== '-' ? 'filled mark-${cell.toLowerCase()}' : ''}`}
                key={index}
                onClick={() => playCell(index)}
                disabled={cell !== '-' || room?.status !== 'IN_PROGRESS' || room?.nextTurn !== session.symbol}
                aria-label={`Cell ${index + 1}`}
              >
                {cell === '-' ? '' : cell}
              </button>
            ))}
          </div>
        </div>

        <aside className="side-panel">
          <div className="connection">
            <span className={connected ? 'dot online' : 'dot'} />
            {connected ? 'Live' : 'Connecting'}
          </div>

          <div className="players">
            <PlayerCard player={room?.xPlayer} symbol="X" active={room?.nextTurn === 'X'} you={session.symbol === 'X'} />
            <PlayerCard player={room?.oPlayer} symbol="O" active={room?.nextTurn === 'O'} you={session.symbol === 'O'} />
          </div>

          <div className="share-box">
            <Users size={21} />
            <div>
              <strong>Room {session.code}</strong>
              <span>Share this code with your friend.</span>
            </div>
          </div>

          {notice && <p className="notice">{notice}</p>}
          <button className="secondary-action" onClick={leaveRoom}>Leave room</button>
        </aside>
      </section>
    </main>
  );
}

function PlayerCard({ player, symbol, active, you }) {
  return (
    <div className={`player-card ${active ? 'active' : ''}`}>
      <span className={`symbol mark-${symbol.toLowerCase()}`}>{symbol}</span>
      <div>
        <strong>{player?.name ?? 'Waiting...'}</strong>
        <span>{you ? 'You' : player ? 'Opponent' : 'Invite pending'}</span>
      </div>
    </div>
  );
}

async function api(path, { method = 'GET', body } = {}) {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    method,
    headers: { 'Content-Type': 'application/json' },
    body: body ? JSON.stringify(body) : undefined
  });
  const data = await response.json();
  if (!response.ok) {
    throw new Error(data.message ?? 'Request failed.');
  }
  return data;
}

function readSession() {
  try {
    return JSON.parse(localStorage.getItem('tic-tac-toe-session'));
  } catch {
    return null;
  }
}

createRoot(document.getElementById('root')).render(<App />);
