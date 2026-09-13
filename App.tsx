import React, {useState} from 'react';
import {View, Text, TouchableOpacity, StyleSheet, NativeModules} from 'react-native';
import {PluginCommAPI, PluginFileAPI, PluginManager, RattaFileSelector} from 'sn-plugin-lib';

const {InsertPageNative} = NativeModules;

type Status = 'idle' | 'working' | 'done' | 'error';

export default function App() {
  const [status, setStatus] = useState<Status>('idle');
  const [message, setMessage] = useState('');

  const getContext = async (): Promise<{filePath: string; pageIndex: number} | null> => {
    console.log('[InsertPage] getContext: calling getCurrentFilePath');
    const pathRes: any = await PluginCommAPI.getCurrentFilePath();
    console.log('[InsertPage] getContext: pathRes=', JSON.stringify(pathRes));
    const filePath: string = pathRes?.success ? pathRes.result : '';

    if (!filePath || !filePath.toLowerCase().endsWith('.pdf')) {
      setStatus('error');
      setMessage("This only works while reading a PDF.");
      return null;
    }

    console.log('[InsertPage] getContext: calling getCurrentPageNum');
    const pageRes: any = await PluginCommAPI.getCurrentPageNum();
    console.log('[InsertPage] getContext: pageRes=', JSON.stringify(pageRes));
    const pageIndex: number =
      pageRes?.success && typeof pageRes.result === 'number' ? pageRes.result : 0;

    return {filePath, pageIndex};
  };

  const insertBlankPage = async () => {
    console.log('[InsertPage] insertBlankPage: button pressed');
    setStatus('working');
    setMessage('');
    try {
      const ctx = await getContext();
      if (!ctx) return;
      const {filePath, pageIndex} = ctx;
      console.log('[InsertPage] insertBlankPage: calling native insertBlankPageAfter');

      await InsertPageNative.insertBlankPageAfter(filePath, pageIndex);
      console.log('[InsertPage] insertBlankPage: native call resolved');

      try {
        await PluginFileAPI.openFile(filePath, pageIndex + 1);
        console.log('[InsertPage] insertBlankPage: openFile resolved');
      } catch (e) {
        console.log('[InsertPage] insertBlankPage: openFile threw', e);
      }

      setStatus('done');
      setMessage('Blank page inserted after page ' + (pageIndex + 1) + '.');

      setTimeout(() => {
        PluginManager.closePluginView().catch(() => {});
      }, 700);
    } catch (err: any) {
      console.log('[InsertPage] insertBlankPage: CAUGHT ERROR', err);
      setStatus('error');
      setMessage(err?.message ?? 'Something went wrong.');
    }
  };

  const insertPdfHere = async () => {
    console.log('[InsertPage] insertPdfHere: button pressed');
    setStatus('working');
    setMessage('');
    try {
      const ctx = await getContext();
      if (!ctx) return;
      const {filePath, pageIndex} = ctx;

      console.log('[InsertPage] insertPdfHere: opening file selector');
      const selected: any = await RattaFileSelector.selectFile({
        selectType: 1,
        suffixList: ['pdf'],
        maxNum: 1,
        title: 'Select PDF to insert',
      });
      console.log('[InsertPage] insertPdfHere: selected=', JSON.stringify(selected));

      if (!selected || selected.length === 0) {
        setStatus('idle');
        setMessage('');
        return;
      }
      const sourcePath = selected[0];

      console.log('[InsertPage] insertPdfHere: calling native insertPdfAfter');
      await InsertPageNative.insertPdfAfter(filePath, pageIndex, sourcePath);
      console.log('[InsertPage] insertPdfHere: native call resolved');

      try {
        await PluginFileAPI.openFile(filePath, pageIndex + 1);
        console.log('[InsertPage] insertPdfHere: openFile resolved');
      } catch (e) {
        console.log('[InsertPage] insertPdfHere: openFile threw', e);
      }

      setStatus('done');
      setMessage('PDF inserted after page ' + (pageIndex + 1) + '.');

      setTimeout(() => {
        PluginManager.closePluginView().catch(() => {});
      }, 700);
    } catch (err: any) {
      console.log('[InsertPage] insertPdfHere: CAUGHT ERROR', err);
      setStatus('error');
      setMessage(err?.message ?? 'Something went wrong.');
    }
  };

  const deleteCurrentPage = async () => {
    console.log('[InsertPage] deleteCurrentPage: button pressed');
    setStatus('working');
    setMessage('');
    try {
      const ctx = await getContext();
      if (!ctx) return;
      const {filePath, pageIndex} = ctx;
      console.log('[InsertPage] deleteCurrentPage: calling native deletePageAt');

      await InsertPageNative.deletePageAt(filePath, pageIndex);
      console.log('[InsertPage] deleteCurrentPage: native call resolved');

      try {
        await PluginFileAPI.openFile(filePath, Math.max(0, pageIndex - 1));
        console.log('[InsertPage] deleteCurrentPage: openFile resolved');
      } catch (e) {
        console.log('[InsertPage] deleteCurrentPage: openFile threw', e);
      }

      setStatus('done');
      setMessage('Page ' + (pageIndex + 1) + ' deleted.');

      setTimeout(() => {
        PluginManager.closePluginView().catch(() => {});
      }, 700);
    } catch (err: any) {
      console.log('[InsertPage] deleteCurrentPage: CAUGHT ERROR', err);
      setStatus('error');
      setMessage(err?.message ?? 'Something went wrong.');
    }
  };

  const undoLastAction = async () => {
    console.log('[InsertPage] undoLastAction: button pressed');
    setStatus('working');
    setMessage('');
    try {
      const ctx = await getContext();
      if (!ctx) return;
      const {filePath, pageIndex} = ctx;
      console.log('[InsertPage] undoLastAction: calling native undoLastAction');

      await InsertPageNative.undoLastAction(filePath);
      console.log('[InsertPage] undoLastAction: native call resolved');

      try {
        await PluginFileAPI.openFile(filePath, pageIndex);
        console.log('[InsertPage] undoLastAction: openFile resolved');
      } catch (e) {
        console.log('[InsertPage] undoLastAction: openFile threw', e);
      }

      setStatus('done');
      setMessage('Last action undone.');

      setTimeout(() => {
        PluginManager.closePluginView().catch(() => {});
      }, 700);
    } catch (err: any) {
      console.log('[InsertPage] undoLastAction: CAUGHT ERROR', err);
      setStatus('error');
      setMessage(err?.message ?? 'Nothing to undo.');
    }
  };

  return (
    <View style={styles.container}>
      <Text style={styles.title}>Insert Page</Text>
      <Text style={styles.subtitle}>
        Add a blank page or another PDF's pages after the page you're on, or delete the page you're on.
      </Text>

      <TouchableOpacity
        style={styles.button}
        onPress={insertBlankPage}
        disabled={status === 'working'}>
        <Text style={styles.buttonText}>
          {status === 'working' ? 'Working…' : 'Insert blank page here'}
        </Text>
      </TouchableOpacity>

      <TouchableOpacity
        style={[styles.button, styles.pdfButton]}
        onPress={insertPdfHere}
        disabled={status === 'working'}>
        <Text style={styles.buttonText}>
          {status === 'working' ? 'Working…' : 'Insert PDF document here'}
        </Text>
      </TouchableOpacity>

      <TouchableOpacity
        style={[styles.button, styles.deleteButton]}
        onPress={deleteCurrentPage}
        disabled={status === 'working'}>
        <Text style={styles.buttonText}>
          {status === 'working' ? 'Working…' : 'Delete this page'}
        </Text>
      </TouchableOpacity>

      <TouchableOpacity
        style={[styles.button, styles.undoButton]}
        onPress={undoLastAction}
        disabled={status === 'working'}>
        <Text style={styles.buttonText}>
          {status === 'working' ? 'Working…' : 'Undo last action'}
        </Text>
      </TouchableOpacity>

      {message ? <Text style={styles.message}>{message}</Text> : null}
    </View>
  );
}

const styles = StyleSheet.create({
  container: {flex: 1, padding: 24, justifyContent: 'center'},
  title: {fontSize: 22, fontWeight: '600', marginBottom: 8},
  subtitle: {fontSize: 14, marginBottom: 24, color: '#333'},
  button: {
    borderWidth: 2,
    borderColor: '#000',
    paddingVertical: 14,
    alignItems: 'center',
    marginBottom: 12,
  },
  deleteButton: {
    borderColor: '#900',
  },
  undoButton: {
    borderColor: '#555',
  },
  pdfButton: {
    borderColor: '#06c',
  },
  buttonText: {fontSize: 16, fontWeight: '600'},
  message: {marginTop: 16, fontSize: 14},
});
