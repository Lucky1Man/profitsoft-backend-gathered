import express from 'express';
import ping from 'src/controllers/ping';
import testimonies from './testimonies';

const router = express.Router();

router.get('/ping', ping);

router.use('/api/v1/testimonies', testimonies);

export default router;
